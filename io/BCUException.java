package common.io;

/**
 * BCU独自の入出力形式に対する契約違反を表すランタイム例外。
 */
public class BCUException extends RuntimeException {

	private static final long serialVersionUID = 1L;

	public BCUException(String str) {
		super(str);
	}

}
