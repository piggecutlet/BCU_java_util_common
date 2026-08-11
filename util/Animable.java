package common.util;

import common.io.json.JsonClass;
import common.io.json.JsonField;
import common.pack.Source.ResourceLocation;
import common.util.anim.AnimI;
import common.util.anim.EAnimI;

/**
 * アニメーション定義を保持し、指定された種別の再生用インスタンスを生成できる画像資源の基底型。
 *
 * @param <A> 保持するアニメーション定義
 * @param <T> アニメーション種別
 */
@JsonClass
public abstract class Animable<A extends AnimI<A, T>, T extends Enum<T> & AnimI.AnimType<A, T>> extends ImgCore {

	@JsonField(alias = ResourceLocation.class)
	public A anim;

	public abstract EAnimI getEAnim(T t);

}
