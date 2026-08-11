package common.battle;

import common.io.json.JsonClass;
import common.io.json.JsonClass.RType;
import common.io.json.JsonField;
import common.util.Data;

/**
 * 戦闘倍率計算が参照する宝物設定とコンボ補正の共通基底。
 * 保存用セットでは補正を無効化し、実戦編成では編成中コンボから補正値を算出する。
 */
@JsonClass(read = RType.FILL)
public abstract class Basis extends Data {

	@JsonField
	public String name;

	/**
	 * 指定種別のコンボ補正値を返す。
	 */
	public abstract int getInc(int type);

	/**
	 * この基礎設定に属する宝物データを返す。
	 */
	public abstract Treasure t();

	@Override
	public String toString() {
		return name;
	}

}
