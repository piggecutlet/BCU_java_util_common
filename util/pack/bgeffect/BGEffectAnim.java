package common.util.pack.bgeffect;

import common.system.VImg;
import common.system.fake.FakeImage;
import common.util.anim.*;

import java.util.ArrayList;
import java.util.List;

/**
 * JSON 背景効果の一要素を描画するため、指定された画像・imgcut・モデル・アニメーションを遅延読み込みする。
 */
public class BGEffectAnim extends AnimD<BGEffectAnim, BGEffectAnim.BGEffType> {

    private final String imgcutName, mamodelName, maanimName;
    private VImg img;

    /**
     * @param st PNG ファイル名
     * @param imgcut imgcut ファイル名
     * @param mamodel mamodel ファイル名
     * @param maanim maanim ファイル名。maanim で終わらない場合は空アニメーションを使用する
     */
    public BGEffectAnim(String st, String imgcut, String mamodel, String maanim) {
        super(st);
        imgcutName = imgcut;
        mamodelName = mamodel;
        maanimName = maanim;

        img = new VImg(str);
    }

    public enum BGEffType implements AnimI.AnimType<BGEffectAnim, BGEffType> {
        DEF
    }

    @Override
    public FakeImage getNum() {
        if (img.getImg().bimg() == null || !img.getImg().isValid())
            img = new VImg(str);

        return img.getImg();
    }

    @Override
    public void load() {
        imgcut = ImgCut.newIns(imgcutName);
        mamodel = MaModel.newIns(mamodelName);

        if (maanimName.endsWith("maanim")) {
            anims = new MaAnim[] { MaAnim.newIns(maanimName) };
        } else {
            anims = new MaAnim[] { new MaAnim() };
        }

        types = BGEffType.values();
        img = new VImg(str);
        parts = imgcut.cut(img.getImg());

        loaded = true;
    }

    @Override
    public boolean cantLoadAll(AnimU.ImageKeeper.AnimationType type) {
        // ゲーム本体の背景効果資源であり、欠落を許容する対象にしない
        return false;
    }

    @Override
    public List<String> collectInvalidAnimation(AnimU.ImageKeeper.AnimationType type) {
        // ゲーム本体の背景効果資源であり、無効アニメーションとして収集しない
        return new ArrayList<>();
    }
}
