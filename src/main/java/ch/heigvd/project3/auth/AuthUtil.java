package ch.heigvd.project3.auth;

import de.mkammerer.argon2.Argon2;
import de.mkammerer.argon2.Argon2Factory;
import io.javalin.http.InternalServerErrorResponse;

public class AuthUtil {
  /**
   * Creates a hash of the given password using Argon2.
   *
   * @param pass the password to hash
   * @return the hashed password
   * @throws InternalServerErrorResponse if hashing fails
   */
  public static String createHash(String pass) {
    Argon2 argon2 = Argon2Factory.create();

    char[] password = pass.toCharArray();
    String hash = argon2.hash(3, 65536, 1, password);

    if (!argon2.verify(hash, password)) throw new InternalServerErrorResponse("Hashing failed.");

    return hash;
  }
}
