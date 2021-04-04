package net.osmand.live.subscriptions;

import net.osmand.util.Algorithms;

import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.text.MessageFormat;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;

public class HuaweiIAPHelper {

	private static final String CLIENT_SECRET = "";
	private static final String CLIENT_ID = "";
	private static final String TOKEN_URL = "https://oauth-login.cloud.huawei.com/oauth2/v3/token";

	/**
	 * The accessToken.
	 */
	private static String accessToken = null;
	private static long accessTokenExpireTime = 0;

	/**
	 * Gets App Level AccessToken.
	 *
	 * @return the App Level AccessToken
	 * @throws Exception the exception
	 */
	private static String getAccessToken() throws Exception {
		if (accessTokenExpireTime < System.currentTimeMillis()) {
			// fetch accessToken
			String grantType = "client_credentials";
			String msgBody = MessageFormat.format("grant_type={0}&client_secret={1}&client_id={2}", grantType,
					URLEncoder.encode(CLIENT_SECRET, "UTF-8"), CLIENT_ID);
			String response =
					httpPost(TOKEN_URL, "application/x-www-form-urlencoded; charset=UTF-8", msgBody, 10000, 10000, null);
			JSONObject obj = new JSONObject(response);
			accessToken = obj.getString("access_token");
			accessTokenExpireTime = System.currentTimeMillis() + obj.getLong("expires_in") * 1000 - 10000;
		}
		return accessToken;
	}

	/**
	 * Build Authorization in Header
	 *
	 * @param appAccessToken appAccessToken
	 * @return headers
	 */
	private static Map<String, String> buildAuthorization(String appAccessToken) {
		String oriString = MessageFormat.format("APPAT:{0}", appAccessToken);
		String authorization =
				MessageFormat.format("Basic {0}", Base64.getEncoder().encodeToString(oriString.getBytes(StandardCharsets.UTF_8)));
		Map<String, String> headers = new HashMap<>();
		headers.put("Authorization", authorization);
		headers.put("Content-Type", "application/json; charset=UTF-8");
		return headers;
	}

	/**
	 * Http post function.
	 *
	 * @param httpUrl        the http url
	 * @param data           the data
	 * @param connectTimeout the connect timeout
	 * @param readTimeout    the read timeout
	 * @param headers        the headers
	 * @return the response as string
	 * @throws IOException the io exception
	 */
	private static String httpPost(String httpUrl, String contentType, String data, int connectTimeout, int readTimeout,
								   Map<String, String> headers) throws IOException {
		OutputStream output = null;
		InputStream in = null;
		HttpURLConnection urlConnection = null;
		BufferedReader bufferedReader = null;
		InputStreamReader inputStreamReader = null;
		try {
			URL url = new URL(httpUrl);
			urlConnection = (HttpURLConnection) url.openConnection();
			urlConnection.setRequestMethod("POST");
			urlConnection.setDoOutput(true);
			urlConnection.setDoInput(true);
			urlConnection.setRequestProperty("Content-Type", contentType);
			if (headers != null) {
				for (String key : headers.keySet()) {
					urlConnection.setRequestProperty(key, headers.get(key));
				}
			}
			urlConnection.setConnectTimeout(connectTimeout);
			urlConnection.setReadTimeout(readTimeout);
			urlConnection.connect();

			// POST data
			output = urlConnection.getOutputStream();
			output.write(data.getBytes(StandardCharsets.UTF_8));
			output.flush();

			// read response
			if (urlConnection.getResponseCode() < 400) {
				in = urlConnection.getInputStream();
			} else {
				in = urlConnection.getErrorStream();
			}

			inputStreamReader = new InputStreamReader(in, StandardCharsets.UTF_8);
			bufferedReader = new BufferedReader(inputStreamReader);
			StringBuilder strBuf = new StringBuilder();
			String str;
			while ((str = bufferedReader.readLine()) != null) {
				strBuf.append(str);
			}
			return strBuf.toString();
		} finally {
			if (bufferedReader != null) {
				bufferedReader.close();
			}
			if (inputStreamReader != null) {
				inputStreamReader.close();
			}
			if (in != null) {
				in.close();
			}
			if (urlConnection != null) {
				urlConnection.disconnect();
			}
		}
	}

	private static void getHuaweiSubscription(String subscriptionId, String purchaseToken)
			throws Exception {
		// fetch the App Level AccessToken
		String accessToken = getAccessToken();
		if (!Algorithms.isEmpty(accessToken)) {
			// construct the Authorization in Header
			Map<String, String> headers = buildAuthorization(accessToken);

			// pack the request body
			Map<String, String> bodyMap = new HashMap<>();
			bodyMap.put("subscriptionId", subscriptionId);
			bodyMap.put("purchaseToken", purchaseToken);

			String msgBody = new JSONObject(bodyMap).toString();

			String response = httpPost("https://subscr-dre.iap.hicloud.com/sub/applications/v2/purchases/get",
					"application/json; charset=UTF-8", msgBody, 10000, 10000, headers);

			System.out.println(response);
		}
	}

	public static void main(String[] args) throws Exception {
		getHuaweiSubscription("1617448653647.42C2D880.1419", "000001789773d34f053a3eeeecff560f1c909c8cbfaa84a827cfd5d7ea146884357d050848424f73x4445.7.1419");
	}

}
