package common.battle.attack;

import common.battle.StageBasis;
import common.system.P;
import common.system.fake.FakeGraphics;
import common.util.BattleObj;

import java.util.Comparator;

/**
 * 複数フレーム存続する攻撃・演出コンテナの基底型。
 * 生成時にステージの保留一覧へ副作用として登録され、描画層順に更新される。
 */
public abstract class ContAb extends BattleObj {

	protected final StageBasis sb;

	public float pos;
	public boolean activate = true;
	public int layer;

	protected ContAb(StageBasis b, float p, int lay) {
		sb = b;
		pos = p;
		layer = lay;
		sb.tlw.add(this);
		sb.tlw.sort(Comparator.comparingInt(e -> e.layer));
	}

	public abstract void draw(FakeGraphics gra, P p, float psiz);

	public abstract void update();

	public abstract void updateAnimation();

	public abstract boolean IMUTime();
}
