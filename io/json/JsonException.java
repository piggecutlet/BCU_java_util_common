package common.io.json;

import com.google.gson.JsonElement;

/**
 * BCUのJSON注釈規約、型、生成関数の不整合を表す検査例外。
 */
public class JsonException extends Exception {

	public enum Type {
		TYPE_MISMATCH, TAG, UNEXPECTED_NULL, FUNC, INTERNAL, UNDEFINED
	}

	private static final long serialVersionUID = 6451473277106188516L;

	public JsonException(Type type, JsonElement elem, String str) {
		super(str);
		// TODO
	}

}
