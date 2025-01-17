package com.example.springredditclone.security;

import com.example.springredditclone.exception.SpringRedditException;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.User;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import java.io.IOException;
import java.io.InputStream;
import java.security.*;
import java.security.cert.CertificateException;
import java.time.Instant;
import java.util.Date;
import static java.util.Date.from;

import static io.jsonwebtoken.Jwts.parser;

@Service
public class JwtProvider {
  private KeyStore keyStore;

  @Value("${jwt.expiration.time}")
  private Long jwtExpirationInMillis;

  // * The PostConstruct annotation is used on a method that needs to be executed
  // * after dependency injection is done to perform any initialization. This
  // * method MUST be invoked before the class is put into service.
  @PostConstruct
  public void init() {
    try {
      keyStore = KeyStore.getInstance("JKS");
      InputStream resourceAsStream = getClass().getResourceAsStream("/springblog.jks");
      keyStore.load(resourceAsStream, "secret".toCharArray());
    } catch (KeyStoreException | CertificateException | NoSuchAlgorithmException | IOException e) {
      throw new SpringRedditException("Exception occurred while loading keystore");
    }
  }

  public String generateToken(Authentication authentication) {
    org.springframework.security.core.userdetails.User principal = (User) authentication.getPrincipal();
    return Jwts.builder()
      .setSubject(principal.getUsername())
      .setIssuedAt(from(Instant.now()))
      .signWith(getPrivateKey())
      .setExpiration(Date.from(Instant.now().plusMillis(jwtExpirationInMillis)))
      .compact();
  }

  public String generateTokenWithUserName(String username) {
    return Jwts.builder()
      .setSubject(username)
      .setIssuedAt(from(Instant.now()))
      .signWith(getPrivateKey())
      .setExpiration(Date.from(Instant.now().plusMillis(jwtExpirationInMillis)))
      .compact();
  }

  private PrivateKey getPrivateKey() {
    try {
      return (PrivateKey) keyStore.getKey("springblog", "secret".toCharArray());
    } catch (KeyStoreException | NoSuchAlgorithmException | UnrecoverableKeyException e) {
      throw new SpringRedditException("Exception occurred while retrieving public key from keystore");
    }
  }

  /**
   * The validateToken method uses the JwtParser class to validate our JWT. If you remember in the previous part,
   * we created our JWT by signing it with the Private Key. Now we can use the corresponding Public Key, to validate the token.
   * */
  public boolean validateToken(String jwt) {
    parser().setSigningKey(getPublickey()).parseClaimsJws(jwt);
    return true;
  }

  private PublicKey getPublickey() {
    try {
      return keyStore.getCertificate("springblog").getPublicKey();
    } catch (KeyStoreException e) {
      throw new SpringRedditException("Exception occured while retrieving public key from keystore");
    }
  }

  public String getUsernameFromJWT(String token) {
    Claims claims = parser()
      .setSigningKey(getPublickey())
      .parseClaimsJws(token)
      .getBody();
    return claims.getSubject();
  }

  public String getUsernameFromJwt(String token) {
    Claims claims = parser()
      .setSigningKey(getPublickey())
      .parseClaimsJws(token)
      .getBody();
    return claims.getSubject();
  }
  public Long getJwtExpirationInMillis() {
    return jwtExpirationInMillis;
  }
}
