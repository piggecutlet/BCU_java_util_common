package common.util.anim;

import common.CommonStatic;
import common.io.json.JsonClass;
import common.pack.Source;
import common.pack.Source.ResourceLocation;
import common.system.VImg;
import common.system.fake.FakeImage;
import common.system.fake.FakeImage.Marker;

import java.util.List;

/**
 * {@link Source.AnimLoader} を取得元とするパック／ワークスペース用アニメーション資源。
 * 資源位置を識別子として保持し、画像をキャッシュしながらモデルとタイムラインの取得・必須ファイル検証を委譲する。
 */
@JsonClass.JCGeneric(ResourceLocation.class)
public class AnimCI extends AnimU<AnimCI.AnimCIKeeper> {

	/**
	 * Sourceローダーとアニメーション層の間で画像キャッシュとマーカー設定を受け持つ。
	 * {@link #unload()} が破棄するのはスプライト画像だけで、アイコン参照は保持される。
	 */
	public static class AnimCIKeeper implements AnimU.ImageKeeper {

		public final Source.AnimLoader loader;
		private FakeImage num;
		private boolean ediLoaded = false;
		private VImg edi;
		private VImg uni;

		private AnimCIKeeper(Source.AnimLoader al) {
			loader = al;
		}

		@Override
		public VImg getEdi() {
			if (ediLoaded && edi != null && edi.getImg().bimg() != null && edi.getImg().isValid())
				return edi;

			ediLoaded = true;

			edi = loader.getEdi();

			if (edi != null)
				edi.mark(Marker.EDI);

			return edi;
		}

		@Override
		public ImgCut getIC() {
			return loader.getIC();
		}

		@Override
		public MaAnim[] getMA() {
			return loader.getMA();
		}

		@Override
		public MaModel getMM() {
			return loader.getMM();
		}

		public ResourceLocation getName() {
			return loader.getName();
		}

		@Override
		public FakeImage getNum() {
			if (num != null && num.bimg() != null && num.isValid())
				return num;

			return num = loader.getNum();
		}

		public int getStatus() {
			return loader.getStatus();
		}

		@Override
		public VImg getUni() {
			if (uni != null && uni.getImg().bimg() != null && uni.getImg().isValid())
				return uni;

			uni = loader.getUni();

			if (uni != null)
				uni.mark(Marker.UNI);
			else
				uni = CommonStatic.getBCAssets().slot[0];
			return uni;
		}

		public void setEdi(VImg vedi) {
			edi = vedi;

			if (vedi != null)
				vedi.mark(Marker.EDI);

			ediLoaded = true;
		}

		public void setNum(FakeImage fimg) {
			num = fimg;
		}

		public void setUni(VImg vuni) {
			uni = vuni;
			uni.mark(Marker.UNI);
		}

		@Override
		public void unload() {
			if(num != null) {
				num.unload();

				num = null;
			}
		}

		@Override
		public boolean validate(AnimationType type) {
			return loader.validate(type);
		}

		@Override
		public List<String> collectInvalidAnimation(AnimationType type) {
			return loader.collectInvalidAnimation(type);
		}
	}

	@JsonClass.JCIdentifier
	public ResourceLocation id;

	public AnimCI(Source.AnimLoader acl) {
		super(new AnimCIKeeper(acl));
		id = loader.getName();
	}

	@Override
	public void load() {
		try {
			super.load();
			if (getEdi() != null)
				getEdi().check();
			if (getUni() != null)
				getUni().check();
		} catch (Exception e) {
			e.printStackTrace();
			CommonStatic.def.save(false, true);
		}
		validate();
	}

	@Override
	public boolean cantLoadAll(ImageKeeper.AnimationType type) {
		return !loader.validate(type);
	}

	@Override
	public List<String> collectInvalidAnimation(ImageKeeper.AnimationType type) {
		return loader.collectInvalidAnimation(type);
	}

	@Override
	public String toString() {
		return id.id;
	}

}