package com.example.slf4jmdcdemo;

import lombok.*;

@AllArgsConstructor
@NoArgsConstructor(access = AccessLevel.PRIVATE)
@Getter
@ToString
@EqualsAndHashCode
public class Employee {
    private String id;
    private String name;
}
