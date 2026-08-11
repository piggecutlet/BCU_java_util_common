package common.io.json;

import com.google.gson.JsonArray;
import common.io.assets.Admin.StaticPermitted;

import java.lang.annotation.Annotation;
import java.lang.annotation.Documented;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;
import java.util.ArrayList;
import java.util.List;

import static java.lang.annotation.ElementType.FIELD;
import static java.lang.annotation.ElementType.METHOD;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

/**
 * BCUの反射ベースJSON変換で、対象メンバーと入出力方向、生成・別名・共有参照の規約を指定する。
 * フィールドではタグ省略時にフィールド名を使うが、メソッドではタグと読み書き片方向の指定が必須。
 * {@link GenType#FILL}は既存オブジェクトまたは配列へ注入し、コレクションには適用されない。
 * {@link GenType#GEN}は1引数コンストラクター、または保持側の公開生成メソッドを使う。
 * 次の組み合わせは使用できない。
 * <ul>
 * <li>プリミティブ型と{@link GenType#FILL}</li>
 * <li>プリミティブ型と{@link GenType#GEN}</li>
 * <li>コレクション型と{@link GenType#FILL}</li>
 * <li>メソッドと{@link GenType#FILL}</li>
 * <li>メソッドと{@link IOType#RW}</li>
 * <li>{@link JsonClass.RType#FILL}と{@link GenType#SET}</li>
 * </ul>
 */
@Documented
@Retention(RUNTIME)
@Target({ FIELD, METHOD })
public @interface JsonField {

	enum GenType {
		SET, FILL, GEN
	}

	/**
	 * 同じインスタンスを参照する配列・リスト要素をプール番号へ置き換え、JSON往復時の共有関係を保つ。
	 */
	@JsonClass
	class Handler {

		public final List<Object> list = new ArrayList<>();

		public Handler() {
		}

		public Handler(JsonArray jarr, Class<?> cls, JsonDecoder dec) throws Exception {
			int n = jarr.size();
			if (dec.curjfld.generic().length == 1)
				cls = dec.curjfld.generic()[0];
			for (int i = 0; i < n; i++)
				list.add(JsonDecoder.decode(jarr.get(i), cls, dec));
		}

		public int add(Object o) {
			if (o == null)
				return -1;
			for (int i = 0; i < list.size(); i++)
				if (list.get(i) == o) // 同一インスタンスとして比較
					return i;
			list.add(o);
			return list.size() - 1;
		}

		public Object get(int i) {
			return i == -1 ? null : list.get(i);
		}

	}

	enum IOType {
		R, W, RW
	}

	enum SerType {
		DEF, FUNC, CLASS
	}

	@StaticPermitted
	JsonField DEF = new JsonField() {

		@Override
		public Class<?>[] alias() {
			return new Class[0];
		}

		@Override
		public Class<? extends Annotation> annotationType() {
			return JsonField.class;
		}

		@Override
		public boolean block() {
			return false;
		}

		@Override
		public GenType gen() {
			return GenType.SET;
		}

		@Override
		public String generator() {
			return "";
		}

		@Override
		public Class<?>[] generic() {
			return new Class[0];
		}

		@Override
		public IOType io() {
			return IOType.RW;
		}

		@Override
		public SerType ser() {
			return SerType.DEF;
		}

		@Override
		public String serializer() {
			return "";
		}

		@Override
		public String tag() {
			return "";
		}

		@Override
		public boolean usePool() {
			return false;
		}

	};

	Class<?>[] alias() default {};

	boolean block() default false;

	/**
	 * フィールド値の生成方法。{@link GenType#SET}は復号値を代入し、
	 * {@link GenType#FILL}は既存値へ注入し、{@link GenType#GEN}は生成メソッドを使う。
	 * メソッドに付ける場合は{@link GenType#SET}のみ。
	 */
	GenType gen() default GenType.SET;

	/**
	 * {@link GenType#GEN}で呼び出す、保持側クラスの公開メソッド名。
	 * 引数は{@code Class}と{@code JsonElement}であり、それ以外の生成方式では使われない。
	 * 省略時は、保持側の型を1引数に取るコンストラクターを探索する。
	 */
	String generator() default "";

	/**
	 * {@link List}、{@link java.util.Set}、{@link java.util.Map}の要素型。
	 * 復号時は宣言されたコレクション型自体も生成可能である必要がある。
	 */
	Class<?>[] generic() default {};

	IOType io() default IOType.RW;

	SerType ser() default SerType.DEF;

	String serializer() default "";

	/**
	 * JSON上のタグ名。フィールドでは省略時にフィールド名、メソッドでは指定必須。
	 */
	String tag() default "";

	boolean usePool() default false;

}