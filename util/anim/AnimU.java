package common.util.anim;

import common.io.assets.Admin.StaticPermitted;
import common.system.VImg;
import common.system.fake.FakeImage;

import java.util.List;

/**
 * ユニット、敵、魂で共通する規約化されたアニメーション資源。
 * {@link ImageKeeper} に実データの取得を委譲し、モデルとタイムラインだけの部分読込と、
 * スプライト分割まで行う完全読込を分離する。
 */
public abstract class AnimU<T extends AnimU.ImageKeeper> extends AnimD<AnimU<?>, AnimU.UType> {

	/**
	 * 編集時にアニメーション種別ごとの回転再生可否を公開する。
	 */
	public interface EditableType {
		boolean rotate();
	}

	/**
	 * 画像、モデル、タイムラインの取得元を抽象化するローダー契約。
	 * 実装は再取得可能なキャッシュの破棄と、用途別の必須ファイル検証も担う。
	 */
	public interface ImageKeeper {
		enum AnimationType {
			SOUL,
			ENEMY,
			UNIT
		}

		VImg getEdi();

		ImgCut getIC();

		MaAnim[] getMA();

		MaModel getMM();

		FakeImage getNum();

		VImg getUni();

		void unload();

		boolean validate(AnimationType type);

		List<String> collectInvalidAnimation(AnimationType type);
	}

	public enum UType implements AnimI.AnimType<AnimU<?>, UType>, EditableType {
		WALK(false), IDLE(false), ATK(true), HB(false), ENTER(true), BURROW_DOWN(true), BURROW_MOVE(false),
		BURROW_UP(true), SOUL(true);

		private final boolean rotate;

		private UType(boolean rotate) {
			this.rotate = rotate;
		}

		@Override
		public boolean rotate() {
			return rotate;
		}
	}

	@StaticPermitted
	public static final UType[] TYPE4 = { UType.WALK, UType.IDLE, UType.ATK, UType.HB };
	@StaticPermitted
	public static final UType[] TYPE5 = { UType.WALK, UType.IDLE, UType.ATK, UType.HB, UType.ENTER };
	@StaticPermitted
	public static final UType[] TYPE7 = { UType.WALK, UType.IDLE, UType.ATK, UType.HB, UType.BURROW_DOWN,
			UType.BURROW_MOVE, UType.BURROW_UP };
	@StaticPermitted
	public static final UType[] SOUL = { UType.SOUL };

	protected boolean partial = false;
	public final T loader;

	protected AnimU(String path, T load) {
		super(path);
		loader = load;
	}

	protected AnimU(T load) {
		super("");
		loader = load;
	}

	public int getAtkLen() {
		partial();
		return anims[2].len + 1;
	}

	@Override
	public EAnimU getEAnim(UType t) {
		check();
		return new EAnimU(this, t);
	}

	public VImg getEdi() {
		return loader.getEdi();
	}

	@Override
	public FakeImage getNum() {
		return loader.getNum();
	}

	public VImg getUni() {
		return loader.getUni();
	}

	@Override
	public void load() {
		loaded = true;
		try {
			imgcut = loader.getIC();
			if (getNum() == null) {
				mamodel = null;
				return;
			}
			parts = imgcut.cut(getNum());
			partial();
		} catch (Exception e) {
			e.printStackTrace();
			loaded = false;
		}
	}

	@Override
	public void unload() {
		loader.unload();
		super.unload();
	}

	public void partial() {
		if (!partial) {
			try {
				partial = true;
				imgcut = loader.getIC();
				mamodel = loader.getMM();
				anims = loader.getMA();
				types = anims.length == 1 ? SOUL : anims.length == 4 ? TYPE4 : anims.length == 5 ? TYPE5 : TYPE7;
			} catch (Exception e) {
				e.printStackTrace();
				partial = false;
			}
		}
	}

}
