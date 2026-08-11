package common.util.stage;

import common.battle.BasisLU;
import common.battle.BasisSet;
import common.battle.Treasure;
import common.io.json.JsonClass;
import common.io.json.JsonField;
import common.util.unit.Form;
import common.util.unit.Level;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * 固定編成ステージが上書きするフォーム、レベル、お宝、にゃんこ砲の定義。
 * 現在編成への適用時に実体を複製し、プリセット定義と戦闘用編成の可変状態を分離する。
 */
@JsonClass(noTag = JsonClass.NoTag.LOAD)
public class BattlePreset {
    public static boolean isCurrentLineupPreset(BattlePreset bp) {
        BasisLU blu = BasisSet.current().sele;
        blu.lu.renew();
        Treasure t = BasisSet.current().t();

        if (!Arrays.equals(t.tech, bp.tech))
            return false;
        else if (!Arrays.equals(t.trea, bp.trea))
            return false;
        else if (!Arrays.equals(t.bslv, bp.bslv))
            return false;
        else if (!Arrays.equals(t.fruit, bp.fruit))
            return false;
        else if (!Arrays.equals(t.gods, bp.gods))
            return false;
        else if (t.alien != bp.alien || t.star != bp.star)
            return false;

        for (int i = 0; i < 3; i++)
            if (bp.nyc[i] != -1 && bp.nyc[i] != blu.nyc[i])
                return false;

        for (int i = 0; i < 2; i++) {
            for (int j = 0; j < 5; j++) {
                Form bpform = bp.fs[i][j];
                Form luform = blu.lu.fs[i][j];
                if (bpform == null || luform == null) {
                    if (bpform != null || luform != null)
                        return false;
                } else if (!bpform.uid.equals(luform.uid) || bpform.fid != luform.fid) {
                    return false;
                } else {
                    Level bplv = bp.levels[i][j];
                    Level lulv = blu.lu.getLv(luform);
                    if (lulv.getLv() != bplv.getLv() || lulv.getPlusLv() != bplv.getPlusLv())
                        return false;
                    else if (!Arrays.equals(Level.getInts(lulv), Level.getInts(bplv)))
                        return false;
                    // todo: オーブの一致判定
                }
            }
        }

        return true;
    }

    public static BattlePreset generatePreset(BasisSet set, boolean treasure, boolean forms, boolean cannon, boolean deco, boolean base) {
        BattlePreset ans = new BattlePreset();
        BasisLU src = set.sele;
        Treasure t = BasisSet.current().t();

        if (treasure) {
            ans.tech = t.tech.clone();
            ans.trea = t.trea.clone();
            ans.bslv = t.bslv.clone();
            ans.fruit = t.fruit.clone();
            ans.gods = t.gods.clone();
            ans.alien = t.alien;
            ans.star = t.star;
        }

        if (forms) {
            for (int i = 0; i < 2; i++) {
                for (int j = 0; j < 5; j++) {
                    Form form = src.lu.fs[i][j];
                    if (form == null)
                        continue;

                    Level lv = src.lu.getLv(form);
                    ans.fs[i][j] = form.unit.forms[form.fid];
                    int[] lvs = new int[10];
                    lvs[0] = lv.getLv();
                    lvs[1] = lv.getPlusLv();
                    System.arraycopy(lv.getTalents(), 0, lvs, 2, lv.getTalents().length);
                    ans.levels[i][j] = Level.lvList(form.unit, lvs, lv.getOrbs());
                }
            }
        }

        for (int i = 0; i < 3; i++)
            if (ans.nyc[i] != -1)
                ans.nyc[i] = src.nyc[i];

        return ans;
    }

    public static void generateBasis(BattlePreset bp) {
        BasisLU dest = BasisSet.current().sele;
        Treasure t = BasisSet.current().t();

        t.tech = bp.tech.clone();
        t.trea = bp.trea.clone();
        t.bslv = bp.bslv.clone();
        t.fruit = bp.fruit.clone();
        t.gods = bp.gods.clone();
        t.alien = bp.alien;
        t.star = bp.star;
        BasisSet.current().renewTreasure();

        for (int i = 0; i < 2; i++) {
            for (int j = 0; j < 5; j++) {
                Form form = bp.fs[i][j];
                Level lv = bp.levels[i][j];
                if (form == null) {
                    dest.lu.fs[i][j] = null;
                    continue;
                }

                dest.lu.fs[i][j] = form.unit.forms[form.fid]; // 正規化したフォーム参照を戦闘用の編成スロットへ設定
                int[] lvs = new int[10];
                lvs[0] = lv.getLv();
                lvs[1] = lv.getPlusLv();
                System.arraycopy(lv.getTalents(), 0, lvs, 2, lv.getTalents().length);
                dest.lu.setLv(form.unit, Level.lvList(form.unit, lvs, lv.getOrbs()));
            }
        }

        for (int i = 0; i < 3; i++)
            if (bp.nyc[i] != -1)
                dest.nyc[i] = bp.nyc[i];

        dest.lu.renew();
    }

    /**
     * プリセットで有効化済みとして表示する章別お宝。
     */
    public enum ActivatedTreasure {
        EOC1,  // 日本編1章
        EOC2,  // 日本編2章
        EOC3,  // 日本編3章
        ITF1,  // 未来編1章
        ITF2,  // 未来編2章
        ITF3,  // 未来編3章
        COTC1, // 宇宙編1章
        COTC2, // 宇宙編2章
        COTC3, // 宇宙編3章
        BASE   // 城体力強化
    }

    /**
     * 公式固定編成データの読み込み中だけ使う形態・基本・プラスレベル。
     */
    public static class LevelObject {
        public int evolution;
        public int level;
        public int plusLevel;
    }
    //TODO カスタム固定編成の読み込みを検証

    @JsonField(alias = Form.FormJson.class)
    public final Form[][] fs = new Form[2][5];
    public final Level[][] levels = new Level[2][5];

    public int baseHealthBoost; // 味方城体力へ加算する値

    // 現在のお宝データから明示的に複製する値
    @JsonField(gen = JsonField.GenType.FILL)
    public int[] tech = new int[Treasure.LV_TOT],
            trea = new int[Treasure.T_TOT],
            bslv = new int[Treasure.BASE_TOT],
            fruit = new int[7],
            gods = new int[3];

    @JsonField(gen = JsonField.GenType.FILL)
    public int[] nyc = new int[] { -1, -1, -1 }; // -1は現在値を置換しない

    @JsonField(block = true)
    public final List<ActivatedTreasure> activatedTreasures = new ArrayList<>(); // 表示用
    @JsonField(block = true)
    public int level; // プリセットは星数ごとに有効化できると思われる

    @JsonField
    public int alien, star;

    @JsonClass.JCConstructor
    public BattlePreset() {

    }

    @Override
    public String toString() {
        return "BattlePreset{\n" +
                "level=" + level + "\n" +
                ", fs=" + Arrays.toString(fs) + "\n" +
                ", levels=" + Arrays.toString(levels) + "\n" +
                ", cannonType=" + Arrays.toString(nyc) + "\n" +
                ", tech=" + Arrays.toString(tech) + "\n" +
                ", trea=" + Arrays.toString(trea) + "\n" +
                ", bslv=" + Arrays.toString(bslv) + "\n" +
                ", fruit=" + Arrays.toString(fruit) + "\n" +
                ", gods=" + Arrays.toString(gods) + "\n" +
                ", activatedTreasures=" + activatedTreasures + "\n" +
                ", alien=" + alien + "\n" +
                ", star=" + star + "\n" +
                '}';
    }
}
