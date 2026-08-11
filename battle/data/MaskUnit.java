package common.battle.data;

import common.util.unit.Form;

/**
 * 戦闘生成に必要な味方ユニット固有データの参照契約。
 * 配置範囲、コスト、再生産時間、才能データをキャラクター共通データへ追加する。
 */
public interface MaskUnit extends MaskEntity {
	int getBack();

	int getFront();

	@Override
	Form getPack();

	int getPrice();

	int getRespawn();

	PCoin getPCoin();

	MaskUnit clone();

	int getLimit();
}
