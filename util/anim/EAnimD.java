package common.util.anim;

import common.CommonStatic;
import common.system.P;
import common.system.fake.FakeGraphics;

/**
 * {@link AnimD} のモデルから作られる、再生時の可変な姿勢と時刻を持つアニメーションインスタンス。
 * 共有タイムラインを評価してインスタンス固有の {@link EPart} 群を更新し、部品のZ値で描画順を組み直す。
 */
@SuppressWarnings("ForLoopReplaceableByForEach")
public class EAnimD<T extends Enum<T> & AnimI.AnimType<?, T>> extends EAnimI {

	public T type;

	protected MaAnim ma;

	public float f = -1f;

	public EAnimD(AnimI<?, T> ia, MaModel mm, MaAnim anim, T t) {
		super(ia, mm);
		ma = anim;
		type = t;
	}

	@SuppressWarnings("unchecked")
	public void changeAnim(T t, boolean skip) {
		f = -1;
		ma = ((AnimD<?, T>) anim()).getMaAnim(t);
		type = t;
		if (skip)
			setTime(0);
	}

	public boolean done() {
		return f >= ma.max;
	}

	@Override
	public void draw(FakeGraphics g, P ori, float siz) {
		if (f == -1) {
			f = 0;
			setup();
		}
		set(g);
		g.translate(ori.x, ori.y);
		for (EPart e : order) {
			P p = P.newP(siz, siz);
			e.drawPart(g, p);
			P.delete(p);
		}
	}

	/**
	 * 指定した不透明度と縦横倍率で全パーツを描画する。
	 * @param g 描画先
	 * @param ori 描画原点
	 * @param siz 基準倍率
	 * @param opacity 不透明度。範囲は0から255
	 * @param sizX 横倍率
	 * @param sizY 縦倍率
	 */
	public void drawBGEffect(FakeGraphics g, P ori, float siz, int opacity, float sizX, float sizY) {
		if(f == -1) {
			f = 0;
			setup();
		}
		set(g);
		g.translate(ori.x, ori.y);
		if(CommonStatic.getConfig().ref) {
			g.colRect(0, 0, 2, 2, 0, 255, 0, 255);
		}
		for (int i = 0; i < order.length; i++) {
			P p = P.newP(siz * sizX, siz * sizY);
			order[i].drawPartWithOpacity(g, p, opacity);
			P.delete(p);
		}
	}

	public float getBaseSizeX() {
		return mamodel.parts[0][8] * 1f / mamodel.ints[0];
	}

	public float getBaseSizeY() {
		return mamodel.parts[0][9] * 1f / mamodel.ints[0];
	}

	public void removeBasePivot() {
		mamodel.parts[0][6] = 0;
		mamodel.parts[0][7] = 0;
	}

	@Override
	public float ind() {
		return f;
	}

	@Override
	public int len() {
		return ma.max + 1;
	}

	@Override
	public void setTime(float value) {
		setup();
		f = value;
		ma.update(f, this, true);
	}

	public void setup() {
		ma.update(0, this, false);
	}

	@Override
	public void update(boolean rotate) {
		if (CommonStatic.getConfig().performanceModeAnimation) {
			if (f == -1) {
				f++;
			} else {
				f += 0.5f;
			}
		} else {
			f++;
		}

		ma.update(f, this, rotate);
	}

	@Override
	protected void performDeepCopy() {
		super.performDeepCopy();
		// 部品配列を再構築した後に時刻を再評価し、コピー元と同じ姿勢へ戻す。
		((EAnimD<?>) copy).setTime(f);
	}

	/**
	 * このアニメーションの基点を別アニメーションの部品へ一時的に接続する。
	 * ワープやノックバックの合成描画後に {@code null} を渡すと元の親へ戻る。
	 */
	public void paraTo(EAnimD<?> base) {
		if (base == null)
			ent[0].setPara(null);
		else
			ent[0].setPara(base.ent[1]);
	}
}
