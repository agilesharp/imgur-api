package me.rabrg.imgur;

import java.io.IOException;

public final class AccountTest {

    public static void main(final String[] args) throws IOException {
        final ImgurAPI api = new ImgurAPI("", "");

        // Gets information about an account
        System.out.println(api.getAccount("rabrg"));
    }
}
