package com.deosound;

import javafx.animation.FadeTransition;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.scene.control.Slider;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import javafx.scene.layout.VBox;
import javafx.scene.media.Media;
import javafx.scene.media.MediaPlayer;
import javafx.scene.text.Text;
import javafx.util.Duration;

import com.google.gson.JsonObject;

import java.net.URL;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.ResourceBundle;

public class TrackController implements Initializable{

    @FXML
    private ListView<HBox> trackListView;
    @FXML
    private ListView<HBox> allListView;
    @FXML
    private ImageView trackImage;
    @FXML
    private Text trackTitle;
    @FXML
    private Text artistName;
    @FXML
    private Text currentTime;
    @FXML
    private Text totalTime;
    @FXML
    private Slider timeSlider;
    @FXML
    private Slider volumeSlider;

    @FXML
    private Pane trackView;
    @FXML
    private Pane allView;
    
    public void initialize(URL location, ResourceBundle resources) {
        // Exibir o trackListView e ocultar o AllListView
    	 allView.setVisible(false);
         allView.setOpacity(0);
         trackView.setVisible(true);
         trackView.setOpacity(1);
         loadAllContent();
    }
    
    private SpotifyService spotifyService;
    private MediaPlayer mediaPlayer;
    private boolean isPlaying = false;
    private String currentTrackUri;

    private List<String> trackUris = new ArrayList<>(); // Lista de URIs das músicas
    private int currentTrackIndex = 0; // Índice da música atual
    private boolean isShuffle = false; // Estado do Shuffle
    private int repeatMode = 0; // 0 = sem repetição, 1 = repetir todas, 2 = repetir atual

    public TrackController() {
        try {
            String accessToken = SpotifyAuth.getAccessToken();
            spotifyService = new SpotifyService(accessToken);
        } catch (Exception e) {
            System.err.println("Erro ao inicializar o SpotifyService.");
            e.printStackTrace();
        }
    }

    public void loadTracks(String playlistId) {
        JsonObject tracksInfo = spotifyService.getTracksFromPlaylist(playlistId);

        if (tracksInfo != null && tracksInfo.has("items")) {
            List<HBox> trackItems = new ArrayList<>();
            trackUris.clear(); // Limpa a lista de URIs ao carregar uma nova playlist

            tracksInfo.getAsJsonArray("items").forEach(item -> {
                try {
                    JsonObject track = item.getAsJsonObject().getAsJsonObject("track");
                    String title = track.get("name").getAsString();
                    String artist = track.getAsJsonArray("artists").get(0).getAsJsonObject().get("name").getAsString();
                    String coverUrl = track.getAsJsonObject("album").get("images").getAsJsonArray().get(0).getAsJsonObject().get("url").getAsString();
                    String trackUri = track.get("preview_url").getAsString();
                    trackUris.add(trackUri); // Adiciona URI à lista
                    HBox trackItem = createTrackItem(title, artist, coverUrl, trackUri);
                    trackItems.add(trackItem);
                } catch (Exception e) {
                    System.err.println("Erro ao processar informações da música.");
                    e.printStackTrace();
                }
            });

            trackListView.getItems().setAll(trackItems);
            
        } else {
            System.out.println("Nenhuma música encontrada na playlist.");
        }
        
    }

    private HBox createTrackItem(String title, String artist, String coverUrl, String trackUri) {
        Label trackTitle = new Label(title);
        Label artistName = new Label(artist);
        ImageView coverImage = new ImageView(new Image(coverUrl, 50, 50, true, true));

        HBox trackItem = new HBox(10, coverImage, trackTitle, artistName);
        trackItem.getStyleClass().add("track-item");

        trackItem.setOnMouseClicked(event -> playTrack(trackUri));

        return trackItem;
    }

    private void playTrack(String trackUri) {
        if (trackUri == null || trackUri.isEmpty()) {
            System.out.println("Não foi possível reproduzir a música: URI inválido.");
            return;
        }

        if (mediaPlayer != null) {
            mediaPlayer.stop();
        }

        try {
            currentTrackUri = trackUri;
            currentTrackIndex = trackUris.indexOf(trackUri);

            // Atualizar a interface com os dados da música
            JsonObject trackInfo = spotifyService.getTrackInfo(trackUri); // Recupera informações da música atual
            if (trackInfo != null) {
                // Extração das informações da música
                String title = trackInfo.get("name").getAsString();
                String artist = trackInfo.getAsJsonArray("artists").get(0).getAsJsonObject().get("name").getAsString();
                String coverUrl = trackInfo.getAsJsonObject("album").get("images").getAsJsonArray().get(0).getAsJsonObject().get("url").getAsString();
                // Atualiza os componentes da interface
                trackTitle.setText(title); // Define o título
                artistName.setText(artist); // Define o nome do artista
                trackImage.setImage(new Image(coverUrl)); // Define a imagem da capa

                System.out.println("Tocando: " + title + " - " + artist);
            }

            // Configuração do player
            Media media = new Media(trackUri);
            mediaPlayer = new MediaPlayer(media);

            // Atualiza o tempo total da música quando a mídia é carregada
            mediaPlayer.setOnReady(() -> {
                double totalSeconds = mediaPlayer.getMedia().getDuration().toSeconds();
                totalTime.setText(formatTime(totalSeconds));
                timeSlider.setMax(totalSeconds);
            });

            // Atualiza o slider e o tempo atual enquanto a música toca
            mediaPlayer.currentTimeProperty().addListener((obs, oldTime, newTime) -> {
                double currentSeconds = newTime.toSeconds();
                currentTime.setText(formatTime(currentSeconds));
                timeSlider.setValue(currentSeconds);
            });

            // Controle para quando o usuário altera o slider manualmente
            timeSlider.valueProperty().addListener((obs, oldVal, newVal) -> {
                if (timeSlider.isValueChanging()) {
                    mediaPlayer.seek(mediaPlayer.getMedia().getDuration().multiply(newVal.doubleValue() / timeSlider.getMax()));
                }
            });

            mediaPlayer.setOnEndOfMedia(() -> handleTrackEnd());
            mediaPlayer.play();
            isPlaying = true;
        } catch (Exception e) {
            System.err.println("Erro ao reproduzir a música.");
            e.printStackTrace();
        }
    }

    /**
     * Formata o tempo em minutos:segundos.
     */
    private String formatTime(double seconds) {
        int minutes = (int) seconds / 60;
        int secs = (int) seconds % 60;
        return String.format("%02d:%02d", minutes, secs);
    }

    private void handleTrackEnd() {
        switch (repeatMode) {
            case 0: // Sem repetição
                System.out.println("Música finalizada. Sem repetição ativa.");
                break;
            case 1: // Repetir todas
                btn_Next();
                break;
            case 2: // Repetir atual
                playTrack(currentTrackUri);
                break;
        }
    }
    
//    Aqui vai a formatação para a aba All
    
    public void loadAllContent() {
        allView.getChildren().clear(); // Limpa o conteúdo existente

        // Recupera os álbuns (mock para exemplo; substitua pelo acesso real à API)
        List<JsonObject> albums = spotifyService.getAlbums("Coldplay"); // Ajuste conforme a sua implementação

        if (albums != null) {
            HBox albumRow = new HBox(20); // Espaçamento horizontal entre os álbuns
            albumRow.setStyle("-fx-padding: 20;"); // Margem opcional
            albumRow.setStyle("-fx-alignment: center;"); // Centraliza os álbuns

            for (JsonObject album : albums) {
                try {
                    // Obtém informações do álbum
                    String albumName = album.get("name").getAsString();
                    String coverUrl = album.getAsJsonArray("images")
                                          .get(0).getAsJsonObject()
                                          .get("url").getAsString();

                    // Cria os elementos visuais para o álbum
                    ImageView albumCover = new ImageView(new Image(coverUrl, 100, 100, true, true));
                    albumCover.getStyleClass().add("album-cover"); // Estilo CSS opcional

                    Label albumLabel = new Label(albumName);
                    albumLabel.getStyleClass().add("album-label"); // Estilo CSS opcional

                    VBox albumBox = new VBox(10, albumCover, albumLabel); // Espaçamento vertical
                    albumBox.setStyle("-fx-alignment: center;"); // Centraliza os elementos no VBox

                    // Adiciona comportamento ao clicar no álbum (ex: mostrar faixas)
                    albumBox.setOnMouseClicked(event -> {
                        System.out.println("Álbum selecionado: " + albumName);
                        // Implementar a navegação para as faixas do álbum
                    });

                    // Adiciona o álbum ao HBox
                    albumRow.getChildren().add(albumBox);
                } catch (Exception e) {
                    System.err.println("Erro ao processar informações do álbum.");
                    e.printStackTrace();
                }
            }

            // Adiciona a linha de álbuns ao `allView`
            allView.getChildren().add(albumRow);
            System.out.println(albumRow);
        } else {
            System.out.println("Nenhum álbum encontrado.");
        }
    }



    @FXML
    private void btn_Shuffle() {
        isShuffle = !isShuffle; // Alterna o estado
        if (isShuffle) {
            Collections.shuffle(trackUris);
            System.out.println("Modo Shuffle ativado.");
        } else {
            loadTracks("playlistId"); // Recarrega a playlist na ordem original
            System.out.println("Modo Shuffle desativado.");
        }
    }

    @FXML
    private void btn_Prev() {
        if (currentTrackIndex > 0) {
            currentTrackIndex--;
            playTrack(trackUris.get(currentTrackIndex));
        } else {
            System.out.println("Nenhuma música anterior disponível.");
        }
    }
    
    @FXML
    private void btn_PlayPause() {
        if (mediaPlayer != null) {
            if (isPlaying) {
                mediaPlayer.pause();
                isPlaying = false;
                System.out.println("Música pausada.");
            } else {
                mediaPlayer.play();
                isPlaying = true;
                System.out.println("Música tocando.");
            }
        } else {
            System.out.println("Nenhuma música está carregada.");
        }
    }

    @FXML
    private void btn_Next() {
        if (currentTrackIndex < trackUris.size() - 1) {
            currentTrackIndex++;
            playTrack(trackUris.get(currentTrackIndex));
        } else if (repeatMode == 1) { // Repetir todas
            currentTrackIndex = 0;
            playTrack(trackUris.get(currentTrackIndex));
        } else {
            System.out.println("Nenhuma próxima música disponível.");
        }
        System.out.println("Título: " + trackTitle);
        System.out.println("Artista: " + artistName);
    }

    @FXML
    private void btn_Repeat() {
        repeatMode = (repeatMode + 1) % 3; // Alterna entre 0, 1 e 2
        switch (repeatMode) {
            case 0:
                System.out.println("Modo Repetição desativado.");
                break;
            case 1:
                System.out.println("Repetindo todas as músicas.");
                break;
            case 2:
                System.out.println("Repetindo a música atual.");
                break;
        }
    }
    @FXML
    private void logoVolume() {
    	
    }
    @FXML
    private void btn_AbaTracks() {
    	  fadeOut((VBox) allView, () -> {
              allView.setVisible(false);
              // Mostrar trackView
              fadeIn((VBox) trackView, () -> trackView.setVisible(true));
          });
    }
    @FXML
    private void btn_AbaAll() {
        fadeOut((VBox) trackView, () -> {
            trackView.setVisible(false);
            // Mostrar allView
            fadeIn((VBox) allView, () -> allView.setVisible(true));
        });
        loadAllContent();
    }

    private void fadeIn(VBox vbox, Runnable onFinish) {
        FadeTransition fadeIn = new FadeTransition(Duration.millis(100), vbox);
        fadeIn.setFromValue(0);
        fadeIn.setToValue(1);
        fadeIn.setOnFinished(event -> onFinish.run());
        fadeIn.play();
    }
    private void fadeOut(VBox vbox, Runnable onFinish) {
        FadeTransition fadeOut = new FadeTransition(Duration.millis(100), vbox);
        fadeOut.setFromValue(1);
        fadeOut.setToValue(0);
        fadeOut.setOnFinished(event -> onFinish.run());
        fadeOut.play();
    }
}
