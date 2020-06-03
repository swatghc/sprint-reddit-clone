package com.example.springredditclone.util;

import lombok.experimental.UtilityClass;

/*
 * A Utility class, by definition, should not contain any state. Hence it is usual to put shared constants or methods
 *  inside utility class so that they can be reused. As they are shared and not tied to any specific
 *  object it makes sense to mark them as static.
 * */


/** @UtilityClass annotation in compile time:
 * Marks the class as final.
 * It generates a private no-arg constructor.
 * It only allows the methods or fields to be static.
 **/
@UtilityClass
public class Constants {
    public static final String ACTIVATION_EMAIL = "http://localhost:8080/api/auth/accountVerification";
}
