package com.github.framiere;

import com.github.javafaker.Faker;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.Wither;
import org.apache.kafka.clients.producer.Producer;
import org.apache.kafka.clients.producer.ProducerRecord;

import java.security.SecureRandom;

import static com.github.framiere.Domain.OperationMode.*;

public class Domain {
    private static final Faker faker = new Faker();
    private static final SecureRandom random = new SecureRandom();

    public interface HasId {
        int getId();
    }

    @Data
    @EqualsAndHashCode(of = "id")
    @NoArgsConstructor
    @AllArgsConstructor
    @Wither
    public static class Member implements HasId {
        public int id;
        public String firstname;
        public String lastname;
        public Gender gender;
        public String phone;
        public MaritalStatus maritalStatus;
        public int teamId;
        public int age;
        public Role role;

        public Member withTeam(Team team) {
            return withTeamId(team.id);
        }
    }

    @Data
    @EqualsAndHashCode(of = "id")
    @NoArgsConstructor
    @AllArgsConstructor
    @Wither
    public static class Address implements HasId {
        public int id;
        public String streetName;
        public String streetAddress;
        public String city;
        public String state;
        public String country;

        public Address changeAddress() {
            return withStreetName(faker.address().streetName())
                    .withStreetAddress(faker.address().streetAddress());
        }

        public Address changeCity() {
            return changeAddress()
                    .withCity(faker.address().city());
        }

        public Address changeState() {
            return changeCity()
                    .withState(faker.address().state());
        }

        public Address changePhone() {
            return withCountry(faker.phoneNumber().phoneNumber());
        }

        public Address changeCountry() {
            return changeState()
                    .changePhone()
                    .withCountry(faker.address().country());
        }
    }

    public enum MaritalStatus {
        MARRIED,
        SINGLE,
        DIVORCED,
        WIDOWED
    }

    public enum Role {
        DEVELOPER,
        QA,
        ARCHITECT,
        MANAGER
    }

    public enum Gender {
        MALE,
        FEMALE,
        THIRD
    }

    @Data
    @EqualsAndHashCode(of = "id")
    @NoArgsConstructor
    @AllArgsConstructor
    @Wither
    public static class Team implements HasId {
        public int id;
        public String name;

        public Team changeName() {
            return withName(faker.team().name());
        }
    }

    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    @Wither
    @EqualsAndHashCode(of = "id")
    public static class Aggregate {
        public Member member;
        public Address address;
        public Team team;
    }

    @AllArgsConstructor
    enum Operation {
        NEW_TEAM(8, INSERT),
        NEW_MEMBER(15, INSERT),
        TEAM_NAME_CHANGE(20, UPDATE),
        DELETE_TEAM(3, DELETE),
        DELETE_MEMBER(4, DELETE),
        NEW_MARITAL_STATUS(5, UPDATE),
        CHANGE_PHONE(2, UPDATE),
        CHANGE_ADDRESS_IN_TOWN(5, UPDATE),
        CHANGE_CITY(4, UPDATE),
        CHANGE_COUNTRY(1, UPDATE),
        CHANGE_GENDER(1, UPDATE),
        CHANGE_TEAM(5, UPDATE),
        CHANGE_ROLE(11, UPDATE),
        ANNIVERSARY(2, UPDATE),
        NO_OP(100, NONE);
        int chanceOfHappening;
        OperationMode operationMode;

        boolean fire() {
            return random.nextInt(100) <= chanceOfHappening;
        }

        public void call(Producer<Integer, Object> producer, HasId object) {
            System.out.println(this + " " + object);
            producer.send(new ProducerRecord<>(object.getClass().getSimpleName(), object.getId(), operationMode == DELETE ? null : object));
            producer.send(new ProducerRecord<>(RandomProducerAction.class.getSimpleName(), new RandomProducerAction(this, object.getClass().getSimpleName(), object)));
        }
    }

    enum OperationMode {
        INSERT,
        DELETE,
        UPDATE,
        NONE
    }

    @AllArgsConstructor
    private static class RandomProducerAction {
        public Operation operation;
        public String clazz;
        public HasId object;
    }
}
