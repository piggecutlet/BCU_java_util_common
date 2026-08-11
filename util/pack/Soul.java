package common.util.pack;

import common.CommonStatic;
import common.io.json.JsonClass;
import common.pack.Identifier;
import common.pack.IndexContainer.IndexCont;
import common.pack.IndexContainer.Indexable;
import common.pack.PackData;
import common.util.Animable;
import common.util.anim.AnimU;
import common.util.anim.EAnimI;
import common.util.stage.Music;

/**
 * パックに保存される魂アニメーションと、その音声・描画レイヤー設定を保持する。
 * Identifier はコンテナ内の同一性に使用され、JSON 読み込み用コンストラクタでは注入まで null になる。
 */
@JsonClass(noTag = JsonClass.NoTag.LOAD)
@IndexCont(PackData.class)
@JsonClass.JCGeneric(Identifier.class)
public class Soul extends Animable<AnimU<?>, AnimU.UType> implements Indexable<PackData, Soul> {

	@JsonClass.JCIdentifier
	private final Identifier<Soul> id;

	public Identifier<Music> audio;
	public String name;
	public int layer_0, layer_1;
	public CommonStatic.LayerType layertype = CommonStatic.LayerType.ORIG;

	@JsonClass.JCConstructor
	public Soul() {
		id = null;
	}

	public Soul(Identifier<Soul> id, AnimU<?> animS) {
		anim = animS;
		this.id = id;

		if (id.pack.equals(Identifier.DEF))
			name = "soul " + id.id;
		else
			name = "custom soul " + id.id;
	}

	@Override
	public Identifier<Soul> getID() {
		return id;
	}

	@Override
	public String toString() {
		return name;
	}

	@Override
	public EAnimI getEAnim(AnimU.UType uType) {
		return anim.getEAnim(uType);
	}
}
