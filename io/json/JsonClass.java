package common.io.json;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import static java.lang.annotation.ElementType.TYPE;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

/**
 * BCUの反射ベースJSON変換へ、生成方法、書き出し方法、無注釈フィールドの扱いを指定する。
 */
@Documented
@Retention(RUNTIME)
@Target(TYPE)
public @interface JsonClass {

	/**
	 * JSON互換のために残されたコンストラクターであることを示す標識。
	 */
	@Target(ElementType.CONSTRUCTOR)
	@interface JCConstructor {
	}

	/**
	 * フィールドの{@code alias}に応じ、別の表現型を介して変換できる型を示す。
	 */
	@Documented
	@Retention(RUNTIME)
	@Target(TYPE)
	@interface JCGeneric {
		Class<?>[] value();
	}

	@Documented
	@Retention(RUNTIME)
	@Target(ElementType.METHOD)
	@interface JCGetter {
	}

	@Documented
	@Retention(RUNTIME)
	@Target(ElementType.FIELD)
	@interface JCIdentifier {
	}

	enum NoTag {
		OMIT, LOAD
	}

	enum RType {
		/**
		 * フィールド側の生成規約が先に適用されない場合、デフォルトコンストラクターで生成してからJSONの値を注入する。
		 */
		DATA,
		/**
		 * 保持側が用意した既存値へJSONの値を注入する。
		 * 既存値を取得できる親コンテキスト、またはフィールド側の{@link JsonField.GenType#FILL}か
		 * {@link JsonField.GenType#GEN}による生成規約が必要。
		 */
		FILL,
		/**
		 * {@link #generator()}で指定した静的メソッドへJSON要素を渡して生成する。
		 */
		MANUAL
	}

	enum WType {
		DEF, CLASS
	}

	/**
	 * 入れ子の変換時に、この型ではなく親のフィールド規約を引き継ぐ。
	 */
	boolean bypass() default false;

	String generator() default "";

	/**
	 * {@link JsonField}のないフィールドを変換対象に含めるか指定する。
	 */
	JsonClass.NoTag noTag() default NoTag.OMIT;

	JsonClass.RType read() default RType.DATA;

	String serializer() default "";

	JsonClass.WType write() default WType.DEF;

}