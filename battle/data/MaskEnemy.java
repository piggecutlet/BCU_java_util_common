package common.battle.data;

import common.battle.Basis;
import common.util.unit.Enemy;

/**
 * 戦闘生成に必要な敵固有データの参照契約。
 * 撃破報酬、倍率種別、出現位置制限をキャラクター共通データへ追加する。
 */
public interface MaskEnemy extends MaskEntity {

	int getDrop();

	@Override
	Enemy getPack();

	int getStar();

	float multi(Basis b);

	float getLimit();
}
