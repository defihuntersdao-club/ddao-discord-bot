package com.justfors.ddaodiscordbot.utils;

import static org.web3j.protocol.http.HttpService.JSON_MEDIA_TYPE;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.math.BigInteger;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import lombok.SneakyThrows;
import okhttp3.Headers;
import okhttp3.OkHttpClient;
import okhttp3.RequestBody;
import okhttp3.ResponseBody;
import okio.Buffer;
import okio.BufferedSource;
import org.web3j.protocol.exceptions.ClientConnectionException;

public final class Web3Client {

	private Web3Client() {
	}

	private static OkHttpClient httpClient = new OkHttpClient();
	private static final boolean includeRawResponse = true;
	private static HashMap<String, String> headers = new HashMap<>();
	private static ObjectMapper objectMapper = new ObjectMapper();

	private static final String RESULT = "result";

	private static String requestTemplate = "{"
			+ "\"jsonrpc\":\"2.0\","
			+ "\"method\":\"eth_call\","
			+ "\"params\":[{\"from\":\"0x7e4e21e00bb52fe49731061b033964c182af9b39\",\"to\":\"0x2856cf575116f7b4fa02ec6c786faa1223b70eef\",\"data\":\"0x70a08231000000000000000000000000{0}\"},"
			+ "\"latest\"],"
			+ "\"id\":0"
			+ "},"
			+ "{\"jsonrpc\":\"2.0\","
			+ "\"method\":\"eth_call\","
			+ "\"params\":[{\"from\":\"0x7e4e21e00bb52fe49731061b033964c182af9b39\",\"to\":\"0x4139ee03053b84280cabe7ede25220edfa484e59\",\"data\":\"0x70a08231000000000000000000000000{0}\"},"
			+ "\"latest\"],"
			+ "\"id\":0"
			+ "},"
			+ "{\"jsonrpc\":\"2.0\","
			+ "\"method\":\"eth_call\","
			+ "\"params\":[{\"from\":\"0x7e4e21e00bb52fe49731061b033964c182af9b39\",\"to\":\"0xd8b355bc31b74feabdedd0e546ddfee9438fb28e\",\"data\":\"0x70a08231000000000000000000000000{0}\"},"
			+ "\"latest\"],"
			+ "\"id\":0"
			+ "},"
			+ "{\"jsonrpc\":\"2.0\","
			+ "\"method\":\"eth_call\","
			+ "\"params\":[{\"from\":\"0x7e4e21e00bb52fe49731061b033964c182af9b39\",\"to\":\"0xcc6ae25446913bf846e1022cde3e3854a9e8ab1e\",\"data\":\"0x70a08231000000000000000000000000{0}\"},"
			+ "\"latest\"],"
			+ "\"id\":0"
			+ "}";

	private static String prepareCheckBlock(String address) {
		String loverCaseAddress = address.toLowerCase().substring(2);
		return requestTemplate.replace("{0}", loverCaseAddress);
	}

	@SneakyThrows
	public static void main(String[] args) {
		String testAddress = "https://matic-mainnet.chainstacklabs.com";
		Map<String, List<Integer>> contranctResults = new HashMap<>();
		List<String> addresses = Arrays.asList(
				"0x5dfcda39199c47a962e39975c92d91e76d16a335",
				"0xB248B3309e31Ca924449fd2dbe21862E9f1accf5"
		);
		Map<String, List<Integer>> contractResults = new HashMap<>();

		int userBatchSize = 4;

		for (int i = 0; i <= addresses.size()/userBatchSize; i++) {
			int finalIndex = i*userBatchSize + userBatchSize;
			List sublist = addresses.subList(i*userBatchSize, Math.min(finalIndex, addresses.size()));
			contractResults.putAll(Web3Client.checkAddresses(sublist, testAddress));
		}

		System.out.println();
	}

	public static Map<String, List<Integer>> checkAddresses(List<String> addresses, String url) throws Exception {
		Map<String, List<Integer>> contractResults = new HashMap<>();

		StringBuilder builder = new StringBuilder();
		builder.append("[");
		for (int i = 0; i < addresses.size(); i++) {
			builder.append(prepareCheckBlock(addresses.get(i)));
			if (i < addresses.size() - 1) {
				builder.append(",");
			}
		}
		builder.append("]");

		InputStream result = performIO(builder.toString(), url);

		List<LinkedHashMap> value = (List<LinkedHashMap>) objectMapper.readValue(result, Object.class);
		for (int i = 0; i < addresses.size(); i++) {
			int localIndex = i * 4;
			contractResults.put(addresses.get(i), Arrays.asList(
					parseResult(value.get(localIndex).get(RESULT)),
					parseResult(value.get(localIndex+1).get(RESULT)),
					parseResult(value.get(localIndex+2).get(RESULT)),
					parseResult(value.get(localIndex+3).get(RESULT))
			));
		}

		return contractResults;
	}

	private static Integer parseResult(Object e) {
		if (e == null) {
			return -1;
		}
		return new BigInteger(((String) e).substring(2)).intValue();
	}

	private static InputStream performIO(String request, String url) throws IOException {

		RequestBody requestBody = RequestBody.create(request, JSON_MEDIA_TYPE);
		Headers headers = buildHeaders();

		okhttp3.Request httpRequest =
				new okhttp3.Request.Builder().url(url).headers(headers)
						.post(requestBody).build();

		okhttp3.Response response = httpClient.newCall(httpRequest).execute();
		ResponseBody responseBody = response.body();
		if (response.isSuccessful()) {
			if (responseBody != null) {
				return buildInputStream(responseBody);
			} else {
				return null;
			}
		} else {
			int code = response.code();
			String text = responseBody == null ? "N/A" : responseBody.string();

			throw new ClientConnectionException("Invalid response received: " + code + "; " + text);
		}
	}

	private static Headers buildHeaders() {
		return Headers.of(headers);
	}

	private static InputStream buildInputStream(ResponseBody responseBody) throws IOException {
		InputStream inputStream = responseBody.byteStream();

		if (includeRawResponse) {
			// we have to buffer the entire input payload, so that after processing
			// it can be re-read and used to populate the rawResponse field.

			BufferedSource source = responseBody.source();
			source.request(Long.MAX_VALUE); // Buffer the entire body
			Buffer buffer = source.buffer();

			long size = buffer.size();
			if (size > Integer.MAX_VALUE) {
				throw new UnsupportedOperationException(
						"Non-integer input buffer size specified: " + size);
			}

			int bufferSize = (int) size;
			BufferedInputStream bufferedinputStream =
					new BufferedInputStream(inputStream, bufferSize);

			bufferedinputStream.mark(inputStream.available());
			return bufferedinputStream;

		} else {
			return inputStream;
		}
	}

}
