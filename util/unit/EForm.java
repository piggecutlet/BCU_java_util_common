package common.util.unit;

import common.CommonStatic;
import common.battle.StageBasis;
import common.battle.data.MaskUnit;
import common.battle.data.PCoin;
import common.battle.entity.EUnit;
import common.util.Data;
import common.util.anim.AnimU;
import common.util.anim.EAnimU;

/**
 * フォーム定義と選択レベルを結び、戦闘用味方エンティティを生成する実行時ラッパー。
 * 本能適用後の能力定義を保持するが、体力や位置など個体状態は生成された{@link EUnit}が保持する。
 */
public class EForm extends Data {

	private final Form f;
	private final Level level;

	public final MaskUnit du;

	public EForm(Form form, int lv) {
		f = form;
		level = new Level(form.du.getPCoin() == null ? 0 : form.du.getPCoin().info.size());
		level.setLevel(lv);

		du = form.du;
	}

	public EForm(Form form, Level level) {
		f = form;
		this.level = level;
		PCoin pc = f.du.getPCoin();
		if (pc != null)
			du = pc.improve(level.getTalents());
		else
			du = form.du;
	}

	/**
	 * 基本レベルとプラス値の合計から成長倍率を求め、必要なら魔界編のレベル制限を先に適用する。
	 */
	public EUnit getEntity(StageBasis b, int[] index, boolean isSpirit, boolean everyOtherOrb) {
		if(b.st.isAkuStage())
			getAkuStageLevel(level);

		float d = f.unit.lv.getMult(level.getLv() + level.getPlusLv());
		EAnimU walkAnim = f.getEAnim(AnimU.UType.WALK);
		walkAnim.setTime(0);

		return new EUnit(b, du, walkAnim, d, du.getFront(), du.getBack(), level, f.du.getPCoin(), index, isSpirit, everyOtherOrb);
	}

	public EUnit invokeEntity(StageBasis b, int Lvl, int minLayer, int maxLayer) {
		float d = f.unit.lv.getMult(Lvl);
		EAnimU walkAnim = f.getEAnim(AnimU.UType.WALK);
		walkAnim.setTime(0);
		return new EUnit(b, du, walkAnim, d, minLayer, maxLayer, level, f.du.getPCoin(), null, false, false);
	}

	public float getPrice(int sta) {
		return du.getPrice() * (1 + sta * 0.5f);
	}

	private void getAkuStageLevel(Level level) {
		if(CommonStatic.getConfig().levelLimit == 0)
			return;

		level.setLevel(Math.min(level.getLv(), CommonStatic.getConfig().levelLimit));
		level.setPlusLevel(CommonStatic.getConfig().plus ? level.getPlusLv() : 0);
	}

	public Level getLevel() {
		return level;
	}
}
