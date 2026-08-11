package common.system;

import common.io.assets.Admin.StaticPermitted;

import java.util.HashMap;
import java.util.Map;

/**
 * 実装型ごとに登録された基底オブジェクトを使って複製を生成する{@link Copable}。
 *
 * @param <T> 複製後の型
 * @param <B> 複製時に参照する基底オブジェクトの型
 */
public interface BasedCopable<T, B> extends Cloneable, Copable<T> {

	@StaticPermitted(StaticPermitted.Type.TEMP)
	Map<Class<?>, Object> map = new HashMap<>();

	@Override
	@SuppressWarnings("unchecked")
	default T copy() {
		B base = (B) map.get(getClass());
		if (base == null)
			return null;
		return copy(base);
	}

	T copy(B b);

}
