package me.rabrg.imgur;

import com.github.scribejava.core.builder.ServiceBuilder;
import com.github.scribejava.core.model.Token;
import com.github.scribejava.core.model.Verifier;
import com.github.scribejava.core.oauth.OAuthService;
import me.rabrg.imgur.account.Account;
import me.rabrg.imgur.account.AccountService;
import me.rabrg.imgur.album.Album;
import me.rabrg.imgur.album.AlbumService;
import me.rabrg.imgur.image.Image;
import me.rabrg.imgur.image.ImageService;
import me.rabrg.imgur.oauth.ImgurOAuthAPI;
import okhttp3.Headers;
import okhttp3.Interceptor;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import retrofit2.Call;
import retrofit2.GsonConverterFactory;
import retrofit2.Response;
import retrofit2.Retrofit;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Base64;

/**
 * The core class of the Imgur API wrapper.
 *
 * @author Ryan Greene
 */
public final class ImgurAPI {

    /**
     * The client id.
     */
    private final String clientId;

    /**
     * The client secret.
     */
    private final String clientSecret;

    /**
     * The Retrofit instance.
     */
    private final Retrofit retrofit;

    /**
     * The AccountService instance.
     */
    private final AccountService accountService;

    /**
     * The AlbumService instance.
     */
    private final AlbumService albumService;

    /**
     * The ImageService instance.
     */
    private final ImageService imageService;

    /**
     * The OAuthService instance.
     */
    private final OAuthService oAuthService;

    /**
     * The access token.
     */
    private Token accessToken;

    /**
     * Constructs a new ImgurAPI instance with the specified client id and client secret.
     *
     * @param clientId     The client id.
     * @param clientSecret The client secret.
     */
    public ImgurAPI(final String clientId, final String clientSecret) {
        this.clientId = clientId;
        this.clientSecret = clientSecret;

        final OkHttpClient client = new OkHttpClient.Builder().addInterceptor(new Interceptor() {
            public okhttp3.Response intercept(final Interceptor.Chain chain) throws IOException {
                final Request request = chain.request();
                final Headers.Builder headersBuilder = request.headers().newBuilder();
                if (accessToken == null)
                    headersBuilder.add("Authorization", "Client-ID " + clientId);
                else
                    headersBuilder.add("Authorization", "Bearer " + accessToken.getToken());
                return chain.proceed(request.newBuilder().headers(headersBuilder.build()).build());
            }
        }).build();
        retrofit = new Retrofit.Builder().baseUrl("https://api.imgur.com/3/")
                .addConverterFactory(GsonConverterFactory.create()).client(client).build();

        accountService = retrofit.create(AccountService.class);
        albumService = retrofit.create(AlbumService.class);
        imageService = retrofit.create(ImageService.class);

        oAuthService = new ServiceBuilder().provider(ImgurOAuthAPI.class).apiKey(clientId)
                .apiSecret(clientSecret).build();
    }

    /**
     * Gets the authorization URL to redirect the user to.
     *
     * @return The authorization URL.
     */
    public String getAuthorizationURL() {
        return oAuthService.getAuthorizationUrl(Token.empty());
    }

    /**
     * Authorizes the API with the pin code obtained by the user.
     *
     * @param pin The pin code.
     */
    public void authorize(final String pin) {
        accessToken = oAuthService.getAccessToken(Token.empty(), new Verifier(pin));
    }

    /**
     * Request standard account information of the specified user.
     *
     * @param username The username of the account.
     * @return Information about the account.
     * @throws IOException If the get fails.
     */
    public Account getAccount(final String username) throws IOException {
        return call(accountService.account(username));
    }

    /**
     * Get information about the specified album.
     *
     * @param id The id of the album.
     * @return Information about the album.
     * @throws IOException If the get fails.
     */
    public Album getAlbum(final String id) throws IOException {
        return call(albumService.album(id));
    }

    /**
     * Gets information about an image with the specified id.
     *
     * @param id The id of the image.
     * @return Information about the image.
     * @throws IOException If the get fails.
     */
    public Image getImage(final String id) throws IOException {
        return call(imageService.image(id));
    }

    /**
     * Uploads a new image from the specified input stream.
     *
     * @param inputStream The input stream of the image.
     * @return Information about the image.
     * @throws IOException If the upload fails.
     */
    public Image uploadImage(final InputStream inputStream) throws IOException {
        return uploadImage(inputStream, null, null, null, null, null);
    }

    /**
     * Uploads a new image from the specified input stream with the optional parameters.
     *
     * @param inputStream The input stream of the image.
     * @param album       The id of the album you want to add the image to. For anonymous albums, album should be the
     *                    deleteHash that is returned at creation.
     * @param type        The type of the file that's being sent; file, base64 or URL
     * @param name        The name of the file.
     * @param title       The title of the image.
     * @param description The description of the image.
     * @return Information about the image.
     * @throws IOException If the upload fails.
     */
    public Image uploadImage(final InputStream inputStream, final String album, final String type,
                             final String name, final String title, final String description) throws IOException {
        final ByteArrayOutputStream output = new ByteArrayOutputStream(inputStream.available());
        final BufferedImage image = ImageIO.read(inputStream);
        ImageIO.write(image, "png", output);
        return uploadImage(Base64.getEncoder().encodeToString(output.toByteArray()), album, type, name, title,
                description);
    }

    /**
     * Uploads a new image from the specified bas64 data or URL.
     *
     * @param image The base64 data or URL the image of the image.
     * @return Information about the image.
     * @throws IOException If the upload fails.
     */
    public Image uploadImage(final String image) throws IOException {
        return uploadImage(image, null, null, null, null, null);
    }

    /**
     * Uploads a new image from the specified bas64 data or URL with the optional parameters.
     *
     * @param image       The base64 data or URL the image of the image.
     * @param album       The id of the album you want to add the image to. For anonymous albums, album should be the
     *                    deleteHash that is returned at creation.
     * @param type        The type of the file that's being sent; file, base64 or URL.
     * @param name        The name of the file
     * @param title       The title of the image.
     * @param description The description of the image.
     * @return Information about the image.
     * @throws IOException If the upload fails.
     */
    public Image uploadImage(final String image, final String album, final String type, final String name,
                             final String title, final String description) throws IOException {
        return call(imageService.upload(image, album, type, name, title, description));
    }

    /**
     * Deletes an image. For an anonymous image, id must be the image's deleteHash. If the image belongs to your account
     * then passing the id of the image is sufficient.
     *
     * @param id The id or deleteHash of the image.
     * @throws IOException If the delete fails.
     */
    public void deleteImage(final String id) throws IOException {
        call(imageService.delete(id));
    }

    /**
     * Updates the title or description of an image. You can only update an image you own and is associated with your
     * account. For an anonymous image, id must be the image's deleteHash.
     *
     * @param id          The id or deleteHash of the image.
     * @param title       The title of the image.
     * @param description The description of the image.
     * @throws IOException If the update fails.
     */
    public void updateImage(final String id, final String title, final String description) throws IOException {
        call(imageService.update(id, title, description));
    }

    /**
     * Gets the data of the specified call and handles any errors which may occur.
     *
     * @param call The call.
     * @param <T>  The data type.
     * @return The data.
     * @throws IOException If the call fails.
     */
    private <T> T call(final Call<Model<T>> call) throws IOException {
        final Response<Model<T>> response = call.execute();
        if (!response.isSuccess())
            throw new IOException("Request was unsuccessful with status: " + response.code()
                    + " and message: " + response.message());
        return response.body().getData();
    }
}
