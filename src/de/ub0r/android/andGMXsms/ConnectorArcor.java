package de.ub0r.android.andGMXsms;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.http.HttpResponse;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpRequestBase;
import org.apache.http.client.utils.URLEncodedUtils;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.message.BasicNameValuePair;

import android.widget.AutoCompleteTextView.Validator;


/**
 * 
 * Connector for arcor.de free sms / payed sms
 * 
 * 
 * @author lado
 * 
 */
public class ConnectorArcor extends Connector {

	private final static Pattern BALANCE_MATCH_PATTERN = Pattern
			.compile("<td.+class=\"txtRed\".*?<b>(\\d{1,2})</b>.+<td.+class=\"txtRed\".*?<b>(\\d{1,})</b>");

	private final static String MATCH_WRONG_USERNAME_OR_PASSWORD = "Der Login ist fehlgeschlagen.";

	private final static String MATCH_LOGIN_SUCCESS = "logout.jsp";

	/** Last access um to reuse httpsession */
	//private long lastAccess = 0;

	private HttpClient client = null;

	// TODO share this. Make it Configurable clobal and local
	private static final String FAKE_USER_AGENT = "Mozilla/5.0 (Windows; U;"
			+ " Windows NT 5.1; de; rv:1.9.0.9) Gecko/2009040821"
			+ " Firefox/3.0.9 (.NET CLR 3.5.30729)";

	private interface URL {
		public static final String Login = "https://www.arcor.de/login/login.jsp";
		public static final String Sms = "https://www.arcor.de/ums/ums_neu_sms.jsp";
	}

	protected ConnectorArcor(String u, String p, short con) {
		super(u, p, con);
	}

	protected boolean login() throws WebSMSException {
		try {
			HttpClient client = getHttpClient();
			HttpPost request = createPOST(URL.Login, getLoginPost());
			HttpResponse response = client.execute(request);
			//if (response.getStatusLine().getStatusCode() == 302) {
				//updateBalance();
				//touchLastAccess();
				//return true;
			//}
			String content = stream2str(response.getEntity().getContent());
			if (content.indexOf(MATCH_LOGIN_SUCCESS) > 0) {
				return true;
			} else if (content.indexOf(MATCH_WRONG_USERNAME_OR_PASSWORD) > 0) {
				// TODO something wrong with username or password. missin,
				// wrong, etc
			}
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return false;
	}
	
	private HttpResponse execute(HttpRequestBase request) throws Exception{
			return getHttpClient().execute(request);
	}

	protected boolean updateBalance() throws WebSMSException {
		try {//TODO better try catch

			HttpResponse response = execute(createGET(URL.Sms, null));

			// TODO check status, consider event based flow, error -> exception
			String content = stream2str(response.getEntity().getContent());

			Matcher m = BALANCE_MATCH_PATTERN.matcher(content);
			if (m.find() == true) {//TODO damn, this code does not work on my device?
				String f = m.group(1);//this ist free sms count
				String p = m.group(2);//this ist payed sms count
				System.out.println(f);
				System.out.println(m);
				pushMessage(WebSMS.MESSAGE_FREECOUNT, f + "f, "+ p + "$");
			}else {
				
			}
			return true;
		} catch (Exception ex) {
			System.out.println(ex.getMessage());
			return false;
		}

	}

	protected boolean sendSms() throws WebSMSException {
		try{
		HttpResponse response = execute(createPOST(URL.Sms, getSmsPost()));
		
		return true;
		}catch(Exception ex){
			return false;
		}
	}

	private HttpClient getHttpClient() {
		if (this.client == null) {
			this.client = new DefaultHttpClient();
		}
		return this.client;
	}

	protected ArrayList<BasicNameValuePair> getLoginPost() {
		ArrayList<BasicNameValuePair> post = new ArrayList<BasicNameValuePair>();
		post.add(new BasicNameValuePair("user_name", this.user));
		post.add(new BasicNameValuePair("password", this.password));
		post.add(new BasicNameValuePair("login", "Login"));
		post.add(new BasicNameValuePair("protocol", "https"));
		post.add(new BasicNameValuePair("info", "Online-Passwort"));
		post.add(new BasicNameValuePair("goto", ""));
		return post;
	}
	

	protected ArrayList<BasicNameValuePair> getSmsPost() {
		ArrayList<BasicNameValuePair> post = new ArrayList<BasicNameValuePair>();
		post.add(new BasicNameValuePair("empfaengerAn", this.to[0]));//TODO this is only test
		post.add(new BasicNameValuePair("emailAdressen", WebSMS.prefsSender)); //TODO customize http://code.google.com/p/websmsdroid/issues/detail?id=42&colspec=ID%20Type%20Status%20Priority%20Product%20Component%20Owner%20Summary#c6
		post.add(new BasicNameValuePair("nachricht", this.text));
		post.add(new BasicNameValuePair("gesendetkopiesms", "on"));//TODO on/off see http://code.google.com/p/websmsdroid/issues/detail?id=42&colspec=ID%20Type%20Status%20Priority%20Product%20Component%20Owner%20Summary#c8
		post.add(new BasicNameValuePair("firstVisitOfPage", "0"));
		post.add(new BasicNameValuePair("part", "0"));
		return post;
	}

	
	private HttpGet createGET(String url,List<BasicNameValuePair> params){
		if(params == null || params.isEmpty()){
			return new HttpGet(url);
		}

		StringBuilder sb = new StringBuilder();
		sb.append(url).append("?");
		for(BasicNameValuePair p : params){
			sb.append(p.getName()).append("=").append(p.getValue());
		}
		return new HttpGet(sb.toString());
	}
	
	private HttpPost createPOST(String url, List<BasicNameValuePair> postData) throws UnsupportedEncodingException{
		HttpPost post = new HttpPost(url);
		post.setHeader("User-Agent", FAKE_USER_AGENT);
		post.setEntity(new UrlEncodedFormEntity(postData));
		return post;
	}
	
	@Override
	protected boolean sendMessage() throws WebSMSException {
		return login() && sendSms();
	}

//	private void touchLastAccess() {
	//	lastAccess = System.currentTimeMillis();
	//}

	//private boolean needLogin() {
		//return (System.currentTimeMillis() - lastAccess) > 1000 * 60 * 10;// TODO
																			// find out
																			// session
																			// timeout
																			// for
																			// arcor
	//}
	@Override
	protected boolean updateMessages() throws WebSMSException {
		return (login() && updateBalance());
	}

}
