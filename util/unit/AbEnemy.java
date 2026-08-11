package common.util.unit;

import common.battle.StageBasis;
import common.battle.entity.EEnemy;
import common.pack.Identifier;
import common.pack.IndexContainer.IndexCont;
import common.pack.IndexContainer.Indexable;
import common.pack.PackData;
import common.system.VImg;

import java.util.Set;

/**
 * 固定敵とランダム敵集合を、ステージ編成から同じ識別子で参照するための契約。
 * 実体生成時には倍率と出現レイヤーを受け取り、候補列挙時には最終的な固定敵集合へ展開する。
 */
@IndexCont(PackData.class)
public interface AbEnemy extends Comparable<AbEnemy>, Indexable<PackData, AbEnemy> {

	@Override
	default int compareTo(AbEnemy e) {
		return getID().compareTo(e.getID());
	}

	EEnemy getEntity(StageBasis sb, Object obj, float mul, float mul1, int d0, int d1, int m, int l);

	VImg getIcon();

	@Override
	Identifier<AbEnemy> getID();

	Set<Enemy> getPossible();

}
