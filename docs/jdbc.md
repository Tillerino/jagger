

<!-- toc -->

- [JDBC](#jdbc)
  * [Basics](#basics)
  * [Containers](#containers)
  * [Parameter binding](#parameter-binding)
  * [Pseudo-properties](#pseudo-properties)
  * [Auto-generating queries](#auto-generating-queries)
    + [The `WHERE` clause](#the-where-clause)
  * [Quoting table and column names](#quoting-table-and-column-names)
  * [Raw ResultSet mapping](#raw-resultset-mapping)

<!-- tocstop -->

# JDBC

Many developers need only minimal ORM functionality.
They prefer writing SQL directly and only want help with the boilerplate of mapping between
`ResultSet`, `PreparedStatement`, and their own classes.

This is the design goal of Jagger JDBC:
- Write native SQL queries
- Map inputs into the `PreparedStatement`
- Map outputs from the resulting `ResultSet`
- Handle common SQL boilerplate with minimal ceremony

Structurally, the code follows Jagger's databind approach: map your own classes from or into `java.sql.Connection`.

## Basics

In principle, Jagger performs two tasks:

- Execute the provided query after mapping the method parameters into the query
- Map the result columns into the return value

```java
// ../jagger-tests/jdbc/src/main/java/org/tillerino/jagger/tests/jdbc/Docs.java#L16-L21

record BasicRecord(int id, String payload) {}

interface Serde {
    @JdbcSelect("SELECT * FROM tablename WHERE id = :param")
    BasicRecord select(Connection c, int param) throws SQLException;
}
```

Generates the following code:

```java
// ../jagger-tests/jdbc/target/generated-sources/annotations/org/tillerino/jagger/tests/jdbc/Docs$SerdeImpl.java#L14-L33

@Generated
public class Docs$SerdeImpl implements Docs.Serde {
  @Override
  public Docs.BasicRecord select(Connection c, int param) throws SQLException {
    try (PreparedStatement ps = c.prepareStatement("SELECT * FROM tablename WHERE id = ?")) {
      ps.setInt(1, param);
      ResultSet rs = ps.executeQuery();
      if (!rs.next()) {
        throw new NoResultException("The query did not return any results.");
      }
      int id = rs.getInt("id"); JdbcHelper.throwOnNull(rs, "id");
      String payload = rs.getString("payload");
      Docs.BasicRecord result = new Docs.BasicRecord(id, payload);
      if (rs.next()) {
        throw new NonUniqueResultException("The query returned more than one result.");
      }
      return result;
    }
  }
}
```

Corresponding `@JdbcInsert` and `@JdbcUpdate` annotations are available and described in later sections.

## Containers

A _select_ method can return the plain object, an `Optional`, a `List`, or an `Iterator`/`Iterable`.

- For the plain object, the generated code ensures that exactly one result is returned from the database,
  or throws an exception.
- For an `Optional`, at most one result may be returned; otherwise an exception is thrown.
- For lists, iterators, and iterables, any number of results is permitted.

When returning an iterator or iterable, the `ResultSet` and `PreparedStatement` cannot be closed immediately.
Instead, they are closed when the iterator is exhausted.

Insert and update statements support single objects as well as any iterable container.

## Parameter binding

For inserts and updates, pass the entire entity to the method rather than individual properties.
Access entity properties using dot notation:

```java
// ../jagger-tests/jdbc/src/main/java/org/tillerino/jagger/tests/jdbc/Docs.java#L24-L25

@JdbcUpdate("UPDATE tablename SET payload = :entity.payload WHERE id = :entity.id")
void update(Connection c, BasicRecord entity) throws SQLException;
```

```java
// ../jagger-tests/jdbc/target/generated-sources/annotations/org/tillerino/jagger/tests/jdbc/Docs$Serde2Impl.java#L12-L19

@Override
public void update(Connection c, Docs.BasicRecord entity) throws SQLException {
  try (PreparedStatement ps = c.prepareStatement("UPDATE tablename SET payload = ? WHERE id = ?")) {
    ps.setObject(1, entity.payload());
    ps.setInt(2, entity.id());
    ps.executeUpdate();
  }
}
```

Jagger will automatically unwrap and iterate over iterables for batch updates:

```java
// ../jagger-tests/jdbc/src/main/java/org/tillerino/jagger/tests/jdbc/Docs.java#L27-L28

@JdbcUpdate("INSERT INTO tablename (id, payload) VALUES (:entities.id, :entities.payload)")
void insert(Connection c, List<BasicRecord> entities) throws SQLException;
```

```java
// ../jagger-tests/jdbc/target/generated-sources/annotations/org/tillerino/jagger/tests/jdbc/Docs$Serde2Impl.java#L21-L31

@Override
public void insert(Connection c, List<Docs.BasicRecord> entities) throws SQLException {
  try (PreparedStatement ps = c.prepareStatement("INSERT INTO tablename (id, payload) VALUES (?, ?)")) {
    for (Docs.BasicRecord item : entities) {
      ps.setInt(1, item.id());
      ps.setObject(2, item.payload());
      ps.addBatch();
    }
    ps.executeBatch();
  }
}
```

## Pseudo-properties

When entities have many properties, specifying each column individually becomes cumbersome and error-prone.
Pseudo-properties simplify this:

| Column property   | Value property   | Used in query like                                       | 
|-------------------|------------------|----------------------------------------------------------|
| `.#insertColumns` | `.#insertValues` | `(entity.#insertColumns) VALUES (:entity.#insertValues)` |
| `.#keyColumns`    | `.#keyValues`    | `WHERE (entity.#keyColumns) = (:entity.#keyValues)`      |
| `.#updateColumns` | `.#updateValues` | `SET (entity.#updateColumns) = (:entity.#updateValues)`  |

These pseudo-properties expand into multiple column names or values:
- `insert` includes all columns to be specified when inserting a new row.
  This excludes database-generated columns or columns excluded by `jakarta.persistence.Columns#insertable` (TODO!)
- `key` includes all columns annotated with `jakarta.persistence.Id`.
- `update` excludes key columns and those excluded by `jakarta.persistence.Columns#updatable` (TODO!)

The following entity has multiple IDs (one database-generated) and multiple non-ID columns:

```java
// ../jagger-tests/jdbc/src/main/java/org/tillerino/jagger/tests/jdbc/Docs.java#L31-L43

@Data
class MultiPropertyPojo {
    @Id
    int parentId;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    int id;

    String payload1;

    String payload2;
}
```

These two queries demonstrate pseudo-property expansion:

```java
// ../jagger-tests/jdbc/src/main/java/org/tillerino/jagger/tests/jdbc/Docs.java#L46-L53

@JdbcUpdate("INSERT INTO tablename (entity.#insertColumns) VALUES (:entity.#insertValues)")
void insert(Connection c, MultiPropertyPojo entity) throws SQLException;

@JdbcUpdate(
        """
        UPDATE tablename SET (entity.#updateColumns) = (:entity.#updateValues)
          WHERE (entity.#keyColumns) = (:entity.#keyValues)""")
void update(Connection c, MultiPropertyPojo entity) throws SQLException;
```

```java
// ../jagger-tests/jdbc/target/generated-sources/annotations/org/tillerino/jagger/tests/jdbc/Docs$Serde3Impl.java#L11-L32

@Override
public void insert(Connection c, Docs.MultiPropertyPojo entity) throws SQLException {
  // Preprocessed: INSERT INTO tablename (parentId, payload1, payload2) VALUES (:entity.#insertValues)
  try (PreparedStatement ps = c.prepareStatement("INSERT INTO tablename (parentId, payload1, payload2) VALUES (?, ?, ?)")) {
    ps.setInt(1, entity.getParentId());
    ps.setObject(2, entity.getPayload1());
    ps.setObject(3, entity.getPayload2());
    ps.executeUpdate();
  }
}

@Override
public void update(Connection c, Docs.MultiPropertyPojo entity) throws SQLException {
  // Preprocessed: UPDATE tablename SET (payload1, payload2) = (:entity.payload1, :entity.payload2) WHERE (entity.#keyColumns) = (:entity.#keyValues)
  try (PreparedStatement ps = c.prepareStatement("UPDATE tablename SET (payload1, payload2) = (?, ?) WHERE (parentId, id) = (?, ?)")) {
    ps.setObject(1, entity.getPayload1());
    ps.setObject(2, entity.getPayload2());
    ps.setInt(3, entity.getParentId());
    ps.setInt(4, entity.getId());
    ps.executeUpdate();
  }
}
```

- `insert` excludes the database-generated `id` property, leaving `parentId`, `payload1`, and `payload2`.
- `update` excludes the two `@Id` columns `parentId` and `id`, leaving `payload1` and `payload2`.
- `key` includes both `@Id` columns `parentId` and `id`.

For clarity, Jagger does not automatically add parentheses around expanded lists.
Ensure that pseudo-properties are wrapped in parentheses as shown above.

## Auto-generating queries

Even with pseudo-properties, queries can become repetitive.
Omitting the query entirely triggers auto-generation:

| Annotation    | Query                                                                                                |
|---------------|------------------------------------------------------------------------------------------------------|
| `@JdbcSelect` | `SELECT * FROM "t"`                                                                                  |
| `@JdbcInsert` | `INSERT INTO "t" ("p.#insertColumns") VALUES (:p.#insertValues)`                                     |
| `@JdbcUpdate` | `UPDATE "t" SET ("p.#updateColumns") = (:p.#updateValues) WHERE ("p.#keyColumns") = (:p.#keyValues)` |

Where `t` is the name of the entity's table and `p` is the name of the parameter passed to the insert/update method.
The table name is specified with `jakarta.persistence.Table` or with `org.tillerino.jagger.annotations.JdbcConfig`.

```java
// ../jagger-tests/jdbc/src/main/java/org/tillerino/jagger/tests/jdbc/Docs.java#L56-L57

@Table(name = "tablename")
record AutoRecord(@Id int id, String payload) {}
```

```java
// ../jagger-tests/jdbc/src/main/java/org/tillerino/jagger/tests/jdbc/Docs.java#L60-L67

@JdbcSelect
AutoRecord select(Connection c) throws SQLException;

@JdbcInsert
void select(Connection c, AutoRecord e) throws SQLException;

@JdbcUpdate
void update(Connection c, AutoRecord e) throws SQLException;
```

The generated query will be included as a comment for easier debugging (rest of the code omitted for brevity):

```java
// ../jagger-tests/jdbc/target/generated-sources/annotations/org/tillerino/jagger/tests/jdbc/Docs$Serde4Impl.java#L16-L18

@Override
public Docs.AutoRecord select(Connection c) throws SQLException {
  // Generated: SELECT * FROM "tablename"
```

```java
// ../jagger-tests/jdbc/target/generated-sources/annotations/org/tillerino/jagger/tests/jdbc/Docs$Serde4Impl.java#L34-L36

@Override
public void select(Connection c, Docs.AutoRecord e) throws SQLException {
  // Generated: INSERT INTO "tablename" ("e.#insertColumns") VALUES (:e.#insertValues)
```

```java
// ../jagger-tests/jdbc/target/generated-sources/annotations/org/tillerino/jagger/tests/jdbc/Docs$Serde4Impl.java#L45-L47

@Override
public void update(Connection c, Docs.AutoRecord e) throws SQLException {
  // Generated: UPDATE "tablename" SET ("e.#updateColumns") = (:e.#updateValues) WHERE ("e.#keyColumns") = (:e.#keyValues)
```

### The `WHERE` clause

Select queries typically require a `WHERE` clause, making the auto-generated `SELECT * FROM tablename` rarely useful.
Specify an expression in `@JdbcSelect(where = ...)` to append to the generated query:

```java
// ../jagger-tests/jdbc/src/main/java/org/tillerino/jagger/tests/jdbc/Docs.java#L69-L70

@JdbcSelect(where = "id = :id")
AutoRecord selectById(Connection c, int id) throws SQLException;
```

```java
// ../jagger-tests/jdbc/target/generated-sources/annotations/org/tillerino/jagger/tests/jdbc/Docs$Serde4Impl.java#L56-L59

@Override
public Docs.AutoRecord selectById(Connection c, int id) throws SQLException {
  // Generated: SELECT * FROM "tablename"
  try (PreparedStatement ps = c.prepareStatement("SELECT * FROM \"tablename\" WHERE id = ?")) {
```

## Quoting table and column names

Jagger preserves quoting of table and column names from your SQL queries to support different database dialects.
This includes quoting of pseudo-properties.
When queries are auto-generated, Jagger quotes table and column names by default using the standard `"`.
For dialects requiring different quote characters (e.g. MySQL without `ANSI` SQL mode), specify it
with `@JdbcConfig(quoteChar = ...`)

```java
// ../jagger-tests/jdbc/src/main/java/org/tillerino/jagger/tests/jdbc/AutoQuerySerde.java#L70-L72

@JdbcConfig(quoteChar = "`")
@JdbcSelect
Optional<JakartaTable> selectJakartaWithQuoteChar(Connection c) throws SQLException;
```

Note how the quote character is preserved through the processing stages:

```java
// ../jagger-tests/jdbc/target/generated-sources/annotations/org/tillerino/jagger/tests/jdbc/AutoQuerySerdeImpl.java#L221-L226

@Override
public void updateJakartaWithQuoteChar(Connection c, AutoQuerySerde.JakartaTable entity) throws
    SQLException {
  // Generated: UPDATE `jakarta_auto` SET (`entity.#updateColumns`) = (:entity.#updateValues) WHERE (`entity.#keyColumns`) = (:entity.#keyValues)
  // Preprocessed: UPDATE `jakarta_auto` SET (`payload`, `payload2`) = (:entity.payload, :entity.payload2) WHERE (`entity.#keyColumns`) = (:entity.#keyValues)
  try (PreparedStatement ps = c.prepareStatement("UPDATE `jakarta_auto` SET (`payload`, `payload2`) = (?, ?) WHERE (`id`) = (?)")) {
```

## Raw ResultSet mapping

When direct SQL execution is required, pass a `ResultSet` instead of a `Connection`.
The method signature only needs to specify `ResultSet` as the parameter:

```java
// ../jagger-tests/jdbc/src/main/java/org/tillerino/jagger/tests/jdbc/DirectResultSetSelectSerde.java#L14-L15

@JdbcSelect
List<Serde.SimpleEntityRecord> listFromResultSet(ResultSet rs) throws SQLException;
```

This generates code that reads directly from the passed `ResultSet`:

```java
// ../jagger-tests/jdbc/target/generated-sources/annotations/org/tillerino/jagger/tests/jdbc/DirectResultSetSelectSerdeImpl.java#L33-L42

@Override
public List<Serde.SimpleEntityRecord> listFromResultSet(ResultSet rs) throws SQLException {
  List<Serde.SimpleEntityRecord> results = new ArrayList<>();
  while (rs.next()) {
    int someId = rs.getInt("someId"); JdbcHelper.throwOnNull(rs, "someId");
    String payload = rs.getString("payload");
    results.add(new Serde.SimpleEntityRecord(someId, payload));
  }
  return results;
}
```

All return types (single, optional, list, iterator/iterable) are supported.
The iterator variant returns a `ResultSetIterator` that holds a reference to the original `ResultSet`.
