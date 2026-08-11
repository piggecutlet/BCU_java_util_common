package common.util.pack.bgeffect;

import common.CommonStatic;
import common.io.json.JsonClass;
import common.pack.Context;
import common.pack.Identifier;
import common.pack.UserProfile;
import common.system.P;
import common.system.VImg;
import common.system.fake.FakeGraphics;
import common.system.files.VFile;
import common.util.Data;
import common.util.anim.ImgCut;
import common.util.pack.Background;

import java.io.IOException;
import java.util.*;

/**
 * 背景効果の資源確認、初期化、更新、および前景・後景描画を定義する基底型。
 * 組み込み効果は共有アセットに一つずつ保持されるため、initialize() で戦闘ごとの可変状態を必ず初期化する。
 */
@JsonClass.JCGeneric(Identifier.class)
@JsonClass
public abstract class BackgroundEffect {
    public static Map<Integer, MixedBGEffect> mixture = new HashMap<>();
    public static int BGHeight = 512;
    public static final int battleOffset = (int) (400 / CommonStatic.BattleConst.ratio);
    public static final List<Integer> jsonList = new ArrayList<>();

    public static final Map<Integer, Integer> oldToNew = new HashMap<>();

    public static void read() {
        CommonStatic.BCAuxAssets asset = CommonStatic.getBCAssets();

        asset.bgEffects.put(Data.BG_EFFECT_STAR, new StarBackgroundEffect());
        asset.bgEffects.put(Data.BG_EFFECT_RAIN, new RainBGEffect(new VImg("./org/battle/a/000_a.png"), ImgCut.newIns("./org/battle/a/000_a.imgcut")));
        asset.bgEffects.put(Data.BG_EFFECT_BUBBLE, new BubbleBGEffect(new VImg("./org/img/bgEffect/bubble02.png")));
        asset.bgEffects.put(Data.BG_EFFECT_FALLING_SNOW, new FallingSnowBGEffect(new VImg("./org/img/bgEffect/bubble03_bg040.png")));
        asset.bgEffects.put(Data.BG_EFFECT_SNOW, new SnowBGEffect());
        asset.bgEffects.put(Data.BG_EFFECT_SNOWSTAR, new SnowStarBGEffect());
        asset.bgEffects.put(Data.BG_EFFECT_BLIZZARD, new BlizzardBGEffect(new VImg("./org/img/bgEffect/bubble03_bg040.png")));
        asset.bgEffects.put(Data.BG_EFFECT_SHINING, new ShiningBGEffect());
        asset.bgEffects.put(Data.BG_EFFECT_BALLOON, new BalloonBGEffect());
        asset.bgEffects.put(Data.BG_EFFECT_ROCK, new RockBGEffect());

        CommonStatic.ctx.noticeErr(() -> {
            VFile vf = VFile.get("./org/data/");

            if(vf != null) {
                Collection<VFile> fileList = vf.list();

                if(fileList != null) {
                    for(VFile file : fileList) {
                        if(file == null)
                            continue;

                        if(file.name.matches("bg\\d+\\.json") && file.getData().size() != 0) {
                            jsonList.add(CommonStatic.parseIntN(file.name));
                        }
                    }
                }
            }

            jsonList.sort(Integer::compareTo);

            int currentSize = asset.bgEffects.size();

            for (Integer id : jsonList) {
                asset.bgEffects.put(id, new JsonBGEffect(id, false));
                UserProfile.getBCData().bgs.getRaw(id).effect = id;
                oldToNew.put(currentSize, id);
                currentSize++;
            }

            asset.bgEffects.replaceAll((i, a) -> {
                if(!(a instanceof JsonBGEffect) || !((JsonBGEffect) a).postNeed)
                    return a;

                try {
                    return new JsonBGEffect(((JsonBGEffect) a).id, true);
                } catch (IOException ignored) {
                    return a;
                }
            });

            for(int i = 0; i < UserProfile.getBCData().bgs.size(); i++) {
                Background bg = UserProfile.getBCData().bgs.get(i);

                if(bg.reference != -1) {
                    Background ref = UserProfile.getBCData().bgs.findByID(bg.reference);

                    if(ref == null)
                        continue;

                    if(bg.effect == -1)
                        bg.effect = ref.effect;
                    else if(bg.effect >= 0 && ref.effect != -1) {
                        if(ref.effect >= 0) {
                            mixture.put(bg.id.id, new MixedBGEffect(asset.bgEffects.get(bg.effect), asset.bgEffects.get(ref.effect)));

                            bg.effect = -bg.id.id;
                        } else if(ref.effect == -ref.id.id && mixture.containsKey(ref.id.id)) {
                            mixture.put(bg.id.id, new MixedBGEffect(asset.bgEffects.get(bg.effect), mixture.get(ref.id.id)));

                            bg.effect = -bg.id.id;
                        } else if(ref.id.id != -ref.id.id || !mixture.containsKey(ref.id.id)) {
                            System.out.println("W/BackgroundEffect::read - Unhandled situation for background effect mixing -> Reference BG ID : "+ref.id.id+" | Mixture contains key : "+mixture.containsKey(ref.id.id));
                        }
                    }
                } else if(bg.id.id == 197) {
                    mixture.put(bg.id.id, new MixedBGEffect(asset.bgEffects.get(bg.effect), asset.bgEffects.get(Data.BG_EFFECT_SNOW)));

                    bg.effect = -bg.id.id;
                }
            }
        }, Context.ErrType.FATAL, "Failed to read bg effect data");
    }

    /** 必要な画像やデータを利用可能な状態にする。 */
    public abstract void check();

    /**
     * エンティティより後ろに効果を描画する。
     * @param g 描画先
     * @param rect 戦闘画面の座標
     * @param siz 戦闘画面の拡大率
     * @param midH Y 軸方向の表示補正
     */
    public abstract void preDraw(FakeGraphics g, P rect, final float siz, final float midH);

    /**
     * エンティティより前に効果を描画する。
     * @param g 描画先
     * @param rect 戦闘画面の座標
     * @param siz 戦闘画面の拡大率
     * @param midH Y 軸方向の表示補正
     */
    public abstract void postDraw(FakeGraphics g, P rect, final float siz, final float midH);

    /**
     * 効果の可変状態を1回更新する。
     * @param w 戦闘座標系での幅
     * @param h ピクセル座標系での高さ
     * @param midH Y 軸方向の表示補正
     */
    public abstract void update(int w, float h, float midH);

    public void updateAnimation(int w, float h, float midH) {
        update(w, h, midH);
    }

    /**
     * 戦闘開始時の可変状態を初期化する。
     * @param w 戦闘座標系での幅
     * @param h ピクセル座標系での高さ
     * @param midH Y 軸方向の表示補正
     * @param bg この効果を使用する背景
     */
    public abstract void initialize(int w, float h, float midH, Background bg);

    public void release() {

    }

    /**
     * 戦闘座標を描画ピクセルへ変換する。
     * @param p 戦闘座標
     * @param siz 戦闘画面の拡大率
     * @return 変換後のピクセル値
     */
    protected int convertP(float p, float siz) {
        return (int) (p * CommonStatic.BattleConst.ratio * siz);
    }

    protected int revertP(float px) {
        return (int) (px / CommonStatic.BattleConst.ratio);
    }
}
