import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.logging.Level;

import javax.servlet.ServletInputStream;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.catalyst.advanced.CatalystAdvancedIOHandler;
import com.zc.auth.connectors.ZCConnection;
import com.zc.component.ZCUserDetail;
import com.zc.component.object.ZCObject;
import com.zc.component.object.ZCRowObject;
import com.zc.component.object.ZCTable;
import com.zc.component.users.ZCUser;
import com.zc.component.zcql.ZCQL;

import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;

import okhttp3.HttpUrl;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;

public class CRMCRUD implements CatalystAdvancedIOHandler {
	private static final Logger LOGGER = Logger.getLogger(CRMCRUD.class.getName());
	private String apiUrl = "https://www.zohoapis.com/crm/v2/Leads";
	private String GET = "GET";
	private String POST = "POST";
	private String PUT = "PUT";
	private String DELETE = "DELETE";
	private String CLIENT_ID = "{{YOUR_CLIENT_ID}}"; //Add your client ID
	private String CLIENT_SECRET = "{{YOUR_CLIENT_SECRET}}"; //Add your client secret
	OkHttpClient client = new OkHttpClient();

	@Override
	@SuppressWarnings("unchecked")
	public void runner(HttpServletRequest request, HttpServletResponse response) throws Exception {
		try {

			String url = request.getRequestURI();
			String method = request.getMethod();

			String responseData = "";
			String recordID = "";
			Pattern p = Pattern.compile("([0-9]+)");
			MediaType mediaType = MediaType.parse("application/json");
			JSONParser jsonParser = new JSONParser();
			JSONObject data = new JSONObject();
			org.json.simple.JSONArray reqData = new org.json.simple.JSONArray();

			//Fetches the Refresh Token by calling the getRefreshToken() function, and inserts it along with the userID in the Token table
			if (Pattern.matches("/generateToken", url) && method.equals(GET)) {

				String code = request.getParameter("code");
				ZCUserDetail details = ZCUser.getInstance().getCurrentUser();

				ZCObject object = ZCObject.getInstance();
				ZCRowObject row = ZCRowObject.getInstance();
				row.set("refresh_token", getRefreshToken(code));
				row.set("userId", details.getUserId());

				ZCTable tab = object.getTable(1824000000686079L); //Replace this with the Table ID of your table 
				tab.insertRow(row);
				response.setStatus(200);
				response.sendRedirect("https://leadmanager-721459367.development.catalystserverless.com/app/index.html"); //Add your app domain

			//Fetches the user details by calling the getUserDetails() function 
			} else if (Pattern.matches("/getUserDetails", url) && method.equals(GET)) {

				ArrayList<ZCRowObject> user = getUserDetails();
				JSONObject resp = new JSONObject();

				if (user.isEmpty()) {
					resp.put("userId", null);
					response.setContentType("application/json");
					response.getWriter().write(resp.toJSONString());
					response.setStatus(200);
				} else {
					resp.put("userId", user.get(0).get("Token", "userId"));
					response.setContentType("application/json");
					response.getWriter().write(resp.toJSONString());
					response.setStatus(200);
				}

			//Executes various APIs to access, add, or modify leads in CRM
			//Fetches all leads
			} else if (Pattern.matches("/crmData", url) && method.equals(GET)) {

				responseData = getResponse(GET, null);

			//Fetches a particular lead
			} else if (Pattern.matches("/crmData/([0-9]+)", url) && method.equals(GET)) {

				Matcher m = p.matcher(url);

				if (m.find()) {
					recordID = m.group(1);
				}
				apiUrl = apiUrl + "/" + recordID;

				responseData = getResponse(GET, null);

			//Adds a new lead
			} else if (Pattern.matches("/crmData", url) && method.equals(POST)) {

				ServletInputStream requestBody = request.getInputStream();

				JSONObject jsonObject = (JSONObject) jsonParser.parse(new InputStreamReader(requestBody, "UTF-8"));

				reqData.add(jsonObject);

				data.put("data", reqData);

				RequestBody body = RequestBody.create(mediaType, data.toString());

				responseData = getResponse(POST, body);

			//Deletes a lead
			} else if (Pattern.matches("/crmData/([0-9]+)", url) && method.equals(DELETE)) {

				Matcher m = p.matcher(url);

				if (m.find()) {
					recordID = m.group(1);
				}
				apiUrl = apiUrl + "/" + recordID;

				responseData = getResponse(DELETE, null);

			//Edits a lead
			} else if (Pattern.matches("/crmData/([0-9]+)", url) && method.equals(PUT)) {

				Matcher m = p.matcher(url);

				if (m.find()) {
					recordID = m.group(1);
				}

				apiUrl = apiUrl + "/" + recordID;

				ServletInputStream requestBody = request.getInputStream();

				JSONObject jsonObject = (JSONObject) jsonParser.parse(new InputStreamReader(requestBody, "UTF-8"));

				reqData.add(jsonObject);

				data.put("data", reqData);

				RequestBody body = RequestBody.create(mediaType, data.toJSONString());

				responseData = getResponse(PUT, body);

			} else {
				LOGGER.log(Level.SEVERE, "Error. Invalid Request"); //The actions are logged. You can check the logs from Catalyst Logs.
				response.setStatus(404);
				responseData = "Error. Invalid Request";
				response.getWriter().write(responseData);
			}
			response.setContentType("application/json");
			response.getWriter().write(responseData);
			response.setStatus(200);
		} catch (Exception e) {
			LOGGER.log(Level.SEVERE, "Exception in CRM Function ", e);
			response.setStatus(500);
			response.getWriter().write(e.toString());
		}
	}

	@SuppressWarnings("unchecked")
	
	//Fetches an Access Token using the Refresh Token
	public String getAccessToken(Long userId) throws Exception {

		JSONObject authJson = new JSONObject();
		JSONObject connectorJson = new JSONObject();

		String query = "SELECT refresh_token FROM Token where UserId=" + userId;
		ArrayList<ZCRowObject> rowList = ZCQL.getInstance().executeQuery(query);

		authJson.put("client_id", CLIENT_ID);
		authJson.put("client_secret", CLIENT_SECRET);
		authJson.put("auth_url", "https://accounts.zoho.com/oauth/v2/token");
		authJson.put("refresh_url", "https://accounts.zoho.com/oauth/v2/token");
		authJson.put("refresh_token", rowList.get(0).get("Token", "refresh_token"));
		connectorJson.put(userId.toString(), authJson);

		return ZCConnection.getInstance(connectorJson).getConnector(userId.toString()).getAccessToken();
	}

	//Fetches the Refresh Token by passing the required details
	public String getRefreshToken(String code) throws Exception {

		HttpUrl.Builder urlBuilder = HttpUrl.parse("https://accounts.zoho.com/oauth/v2/token").newBuilder();
		urlBuilder.addQueryParameter("code", code);
		urlBuilder.addQueryParameter("client_id", CLIENT_ID);
		urlBuilder.addQueryParameter("client_secret", CLIENT_SECRET);
		urlBuilder.addQueryParameter("grant_type", "authorization_code");
		urlBuilder.addQueryParameter("redirect_uri",
				"https://leadmanager-721459367.development.catalystserverless.com/server/crmCRUD/generateToken"); //Add your app domain

		String URL = urlBuilder.build().toString();
		MediaType mediaType = MediaType.parse("text/plain");
		RequestBody body = RequestBody.create(mediaType, "");
		Request getResponse = new Request.Builder().url(URL).method(POST, body).build();

		JSONParser jsonParser = new JSONParser();
		JSONObject data = (JSONObject) jsonParser.parse(client.newCall(getResponse).execute().body().string());

		return data.get("refresh_token").toString();
	}

	//Passes the Access Token fetched to obtain the authorization needed to perform each action on the CRM module
	public String getResponse(String METHOD, RequestBody body) throws Exception {

		Long userId = ZCUser.getInstance().getCurrentUser().getUserId();
		String accessToken = getAccessToken(userId);

		Request getResponse = new Request.Builder().url(apiUrl).method(METHOD, body)
				.addHeader("Authorization", "Zoho-oauthtoken " + accessToken).build();

		return client.newCall(getResponse).execute().body().string();

	}

	//Fetches the record from the Token table that contains the Refresh Token, by passing the userID
	public ArrayList<ZCRowObject> getUserDetails() throws Exception {

		Long userId = ZCUser.getInstance().getCurrentUser().getUserId();

		String query = "SELECT * FROM Token where UserId=" + userId;
		ArrayList<ZCRowObject> rowList = ZCQL.getInstance().executeQuery(query);
		return rowList;
	}
}
