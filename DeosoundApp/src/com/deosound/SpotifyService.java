package com.deosound;

import com.google.gson.JsonObject;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import com.google.gson.JsonParser;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;

public class SpotifyService {
    private String accessToken;

    public SpotifyService(String accessToken) {
        this.accessToken = accessToken;
    }

    // Obtém informações sobre uma faixa
    public JsonObject getTrackInfo(String trackId) {
        return getSpotifyData("https://api.spotify.com/v1/tracks/" + trackId);
    }

    // Obtém as faixas de uma playlist
    public JsonObject getTracksFromPlaylist(String playlistId) {
        return getSpotifyData("https://api.spotify.com/v1/playlists/" + playlistId + "/tracks");
    }

    // Método genérico para buscar dados no Spotify
    private JsonObject getSpotifyData(String urlString) {
        try {
            HttpURLConnection connection = (HttpURLConnection) new URL(urlString).openConnection();
            connection.setRequestMethod("GET");
            connection.setRequestProperty("Authorization", "Bearer " + accessToken);
            connection.setRequestProperty("Content-Type", "application/json");

            // Verificar a resposta da API
            int responseCode = connection.getResponseCode();
            if (responseCode == HttpURLConnection.HTTP_OK) {
                BufferedReader reader = new BufferedReader(new InputStreamReader(connection.getInputStream()));
                StringBuilder response = new StringBuilder();
                String line;

                while ((line = reader.readLine()) != null) {
                    response.append(line);
                }
                reader.close();
                return JsonParser.parseString(response.toString()).getAsJsonObject();
            } else {
                System.err.println("Erro na API: " + responseCode);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        return null;
    }

    // Realiza uma busca na API do Spotify (pode ser por faixa, álbum, artista ou playlist)
    public JsonObject search(String type, String query) {
        try {
            // Validar o tipo de busca
            if (!type.matches("track|album|artist|playlist")) {
                throw new IllegalArgumentException("Tipo inválido: " + type);
            }

            // Codificar a consulta
            String encodedQuery = URLEncoder.encode(query, StandardCharsets.UTF_8.toString());
            String url = "https://api.spotify.com/v1/search?q=" + encodedQuery + "&type=" + type;

            // Log da URL
            System.out.println("URL gerada: " + url);

            return getSpotifyData(url);
        } catch (Exception e) {
            e.printStackTrace();
        }

        return null;
    }

    /**
     * Obtém uma lista de álbuns com base no nome do artista ou em um termo de busca.
     *
     * @param query Termo de busca para álbuns ou artista.
     * @return Lista de JsonObject contendo informações dos álbuns.
     */
    public List<JsonObject> getAlbums(String query) {
        try {
            // Codificar a consulta
            String encodedQuery = URLEncoder.encode(query, StandardCharsets.UTF_8.toString());
            String url = "https://api.spotify.com/v1/search?q=" + encodedQuery + "&type=album&limit=4";  // Adiciona o parâmetro 'limit' para limitar a 5 álbuns

            // Chama o método genérico de requisição
            JsonObject responseObject = getSpotifyData(url);
            List<JsonObject> albums = new ArrayList<>();

            if (responseObject != null && responseObject.has("albums")) {
                responseObject.getAsJsonObject("albums")
                        .getAsJsonArray("items")
                        .forEach(item -> albums.add(item.getAsJsonObject()));
            }

            // Limitar a lista de álbuns a 5 (caso a API retorne mais de 5)
            return albums.size() > 5 ? albums.subList(0, 5) : albums;

        } catch (Exception e) {
            e.printStackTrace();
        }

        return null;
    }


    // Testar o método getAlbums
    public void testGetAlbums() {
        SpotifyService spotifyService = new SpotifyService(accessToken); // Substitua por um token válido
        List<JsonObject> albums = spotifyService.getAlbums("Coldplay");

        if (albums != null) {
            for (JsonObject album : albums) {
                System.out.println("Album: " + album.get("name").getAsString());
            }
        } else {
            System.out.println("Nenhum álbum encontrado.");
        }
    }

    // Método genérico para pegar álbuns de um artista específico (caso a URL seja alterada no futuro)
    public List<JsonObject> getAlbumsByArtistId(String artistId) {
        String url = "https://api.spotify.com/v1/artists/" + artistId + "/albums";
        JsonObject responseObject = getSpotifyData(url);
        List<JsonObject> albums = new ArrayList<>();

        if (responseObject != null && responseObject.has("items")) {
            responseObject.getAsJsonArray("items").forEach(item -> albums.add(item.getAsJsonObject()));
        }

        return albums;
    }
}
