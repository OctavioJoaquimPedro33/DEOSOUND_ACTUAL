package com.deosound;

import java.io.*;
import java.net.*;
import java.nio.charset.StandardCharsets;
import java.util.Base64;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

public class SpotifyAuth {
    private static final String CLIENT_ID = "40867f3ae1324aa99c4d8c3ce59df1bc"; // Substitua com seu Client ID
    private static final String CLIENT_SECRET = "e8485ef65c5b4f0aabbb8428b874b9bc"; // Substitua com seu Client Secret
    private static final String TOKEN_URL = "https://accounts.spotify.com/api/token";
    private static String accessToken;

    public static String getAccessToken() throws IOException {
        // Se o token já estiver válido, retorna ele diretamente
        if (accessToken != null && !isTokenExpired()) {
            return accessToken;
        }

        String credentials = CLIENT_ID + ":" + CLIENT_SECRET;
        String encodedCredentials = Base64.getEncoder().encodeToString(credentials.getBytes(StandardCharsets.UTF_8));
        
        HttpURLConnection connection = (HttpURLConnection) new URL(TOKEN_URL).openConnection();
        connection.setRequestMethod("POST");
        connection.setRequestProperty("Authorization", "Basic " + encodedCredentials);
        connection.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");
        connection.setDoOutput(true);

        String body = "grant_type=client_credentials";
        try (OutputStream os = connection.getOutputStream()) {
            os.write(body.getBytes(StandardCharsets.UTF_8));
        }

        try (BufferedReader br = new BufferedReader(new InputStreamReader(connection.getInputStream(), StandardCharsets.UTF_8))) {
            JsonObject jsonResponse = JsonParser.parseReader(br).getAsJsonObject();
            accessToken = jsonResponse.get("access_token").getAsString();
            return accessToken;
        } catch (Exception e) {
            System.err.println("Erro ao obter token de acesso: " + e.getMessage());
            throw new IOException("Falha na autenticação com Spotify", e);
        }
    }

    // Método para verificar se o token está expirado
    private static boolean isTokenExpired() {
        // Aqui você pode adicionar a lógica para verificar a expiração do token,
        // por exemplo, verificando a data de expiração ou o código de erro da API.
        return false; // Para simplicidade, assumimos que o token nunca expira
    }
}
