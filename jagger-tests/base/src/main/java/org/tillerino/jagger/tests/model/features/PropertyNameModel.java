package org.tillerino.jagger.tests.model.features;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.persistence.Column;
import jakarta.persistence.Id;
import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

public interface PropertyNameModel {
    record JsonPropertyCustomName(@JsonProperty("notS") String s) {}

    @EqualsAndHashCode
    class JsonPropertyCustomNameWithSetter {
        private @JsonProperty("notS") String s;

        public JsonPropertyCustomNameWithSetter(String s) {
            this.s = s;
        }

        public JsonPropertyCustomNameWithSetter() {}

        public String getS() {
            return s;
        }

        public void setS(String s) {
            this.s = s;
        }
    }

    @jakarta.persistence.Table(name = "custom_columns")
    record JdbcCustomColumnRecord(
            @Id @Column(name = "custom_id") int id, @Column(name = "custom_payload") String payload) {}

    @jakarta.persistence.Table(name = "custom_columns_pojo")
    @EqualsAndHashCode
    @NoArgsConstructor
    @AllArgsConstructor
    class JdbcCustomColumnPojo {
        private @Id @Column(name = "custom_id") int id;
        private @Column(name = "custom_payload") String payload;

        public int getId() {
            return id;
        }

        public void setId(int id) {
            this.id = id;
        }

        public String getPayload() {
            return payload;
        }

        public void setPayload(String payload) {
            this.payload = payload;
        }
    }
}
