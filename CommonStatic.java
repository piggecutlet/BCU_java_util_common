package common;

import common.io.assets.Admin.StaticPermitted;
import common.io.json.JsonClass;
import common.io.json.JsonClass.NoTag;
import common.io.json.JsonField;
import common.pack.Context;
import common.pack.Identifier;
import common.pack.UserProfile;
import common.system.VImg;
import common.system.fake.FakeImage;
import common.util.Data;
import common.util.anim.ImgCut;
import common.util.anim.MaModel;
import common.util.pack.EffAnim.EffAnimStore;
import common.util.pack.NyCastle;
import common.util.pack.bgeffect.BackgroundEffect;
import common.util.stage.Music;
import common.util.unit.UnitLevel;

import java.io.File;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static java.lang.Character.isDigit;

/**
 * 実行環境に依存するサービスと、ユーザープロファイルに属する共有設定・補助素材への入口をまとめる。
 * {@link #ctx} と {@link #def} は利用前にアプリケーション側で設定する必要がある。
 */
public class CommonStatic {

	public enum LayerType {
		ORIG, SET, RELATIVE;

		@Override
		public String toString() {
			return name();
		}
	}

	/**
	 * 戦闘内座標を描画座標へ換算するための共通定数。
	 */
	public interface BattleConst {

		float ratio = 768f / 2400f;// 描画座標 / 戦闘内座標
	}

	/**
	 * 標準素材の読み込み後に共有される、戦闘・編集画面向けの補助素材群。
	 */
	public static class BCAuxAssets {

		// Res が利用する素材
		public VImg[] slot = new VImg[3];
		public VImg[][] ico = new VImg[2][];
		public VImg[][] num = new VImg[9][11];
		public VImg[][] battle = new VImg[3][];
		public VImg[][] icon = new VImg[5][];
		public VImg[] timer = new VImg[11];
		public VImg emptyEdi = null;

		public Map<Integer, VImg> gatyaitem = new HashMap<>();
		public VImg XP;
		public VImg[][] moneySign = new VImg[4][4]; // 所持金の有効・無効 / コストの有効・無効
		public VImg[] spiritSummon = new VImg[4];
		/**
		 * 属性アイコンがない場合に使用する。
		 */
		public VImg dummyTrait;

		// 背景素材
		public final List<ImgCut> iclist = new ArrayList<>();

		// GUI で利用可能な本能玉データ
		// Map<種類, Map<属性, 等級>>
		public final Map<Integer, Map<Integer, List<Integer>>> ORB = new TreeMap<>();
		public final Map<Integer, Integer> DATA = new HashMap<>();

		// 0 = 大サイズ、1 = 小サイズ
		public FakeImage[][] TYPES = new FakeImage[2][];
		public FakeImage[][] TRAITS = new FakeImage[2][];;
		public FakeImage[][] GRADES = new FakeImage[2][];;

		// NyCastle
		public final VImg[][] main = new VImg[3][NyCastle.TOT];
		public final NyCastle[] atks = new NyCastle[NyCastle.TOT];

		// EffAnim
		public final EffAnimStore effas = new EffAnimStore();

		public final int[][] values = new int[Data.C_TOT][6];
		public int[][] filter;

		public final VImg[] rarity = new VImg[6];
		public final VImg[] maxcat = new VImg[12];

		// 味方キャラクター画像の切り出し定義
		public ImgCut unicut, udicut;

		// RandStage
		public final int[][] randRep = new int[5][];

		// 味方キャラクターの既定レベル
		public UnitLevel defLv;

		// 背景エフェクト
		public final Map<Integer, BackgroundEffect> bgEffects = new HashMap<>();

	}

	/**
	 * ユーザープロファイルに保存され、画面表示・戦闘・バックアップ処理から共有される実行設定。
	 */
	@JsonClass(noTag = NoTag.LOAD)
	public static class Config {

		@JsonField(generic = String.class)
		public ArrayList<String> receivedAnnouncements = new ArrayList<>();

		@JsonField(generic = { String.class, String.class })
		public HashMap<String, String> localLangMap = new HashMap<>();

		@JsonField(generic = { Integer.class, String.class })
		public HashMap<Integer, String> localMusicMap = new HashMap<>();

		// ImgCore
		public int deadOpa = 10, fullOpa = 90;
		public int[] ints = new int[] { 1, 1, 1, 2 };
		public boolean ref = true, battle = false, icon = false;
		public boolean twoRow = true;
		/**
		 * 悪魔編でプラスレベルを有効にするかどうか。
		 */
		public boolean plus = false;
		/**
		 * 悪魔編で適用するレベル上限。0 の場合は制限しない。
		 */
		public int levelLimit = 0;
		// 表示言語
		public Lang.Locale lang = Lang.Locale.EN;
		/**
		 * 復元対象のバックアップファイル名。{@code null} は対象なし。
		 */
		public String backupFile;
		/**
		 * 部分復元の対象パス。{@code null} の場合はバックアップ全体を復元する。
		 */
		public String backupPath;
		/**
		 * バックアップの最大数。0 は上限なし、-1 はバックアップ無効。
		 */
		public int maxBackup = 5;

		/**
		 * 味方キャラクターに適用する優先レベル。
		 */
		public int prefLevel = 50;

		/**
		 * 背景エフェクトを描画するかどうか。
		 */
		public boolean drawBGEffect = true;

		/**
		 * 生産時のボタンに6フレームの待ち時間を適用するかどうか。
		 */
		public boolean buttonDelay = true;

		/**
		 * ビューアーの背景色。-1 の場合は既定色。
		 */
		public int viewerColor = -1;

		/**
		 * EXステージへの続行ポップアップを表示するかどうか。
		 */
		public boolean exContinuation = false;

		/**
		 * Make EX stage pop-up shown considering real chance
		 */
		public boolean realEx = false;

		/**
		 * 戦闘中にステージ名画像を表示するかどうか。
		 */
		public boolean stageName = true;

		/**
		 * 戦闘画面の揺れを有効にするかどうか。
		 */
		public boolean shake = true;

		/**
		 * 更新時に既存の音楽ファイルを置き換えるかどうか。
		 */
		public boolean updateOldMusic = true;

		/**
		 * 本編相当のレベル計算を使用するかどうか。
		 */
		public boolean realLevel = false;

		/**
		 * アニメーション表示・録画を60 FPSで処理するかどうか。
		 */
		public boolean performanceModeAnimation = false;

		/**
		 * 戦闘を60 FPSで処理するかどうか。
		 */
		public boolean performanceModeBattle = false;
	}

	public interface EditLink {

		void review();

	}

	public interface FakeKey {

		boolean pressed(int i, int j);

		void remove(int i, int j);

	}

	/**
	 * 保存・終了と音声再生を実行環境へ委譲するためのインターフェース。
	 */
	public interface Itf {

		/**
		 * 保存処理を行い、指定された場合はアプリケーションを終了する。
		 */
		void save(boolean save, boolean exit);

		long getMusicLength(Music f);

		@Deprecated
		File route(String path);

		void setSE(int mus);

		void setSE(Identifier<Music> mus);

		void setBGM(Identifier<Music> mus);
	}

	/**
	 * 対応言語と、翻訳がない場合の言語フォールバック順を保持する。
	 */
	public static class Lang {
		public enum Locale {
			EN("en"),
			ZH("zh"),
			KR("kr"),
			JP("jp"),
			RU("ru"),
			DE("de"),
			FR("fr"),
			ES("es"),
			IT("it"),
			TH("th");

			public final String code;

			Locale(String localeCode) {
				code = localeCode;
			}

			@Override
			public String toString() {
				return code;
			}
		}

		/**
		 * 各表示言語で翻訳を探索する優先順。
		 */
		@StaticPermitted
		public static final Locale[][] pref = {
				{ Locale.EN, Locale.FR, Locale.IT, Locale.ES, Locale.DE, Locale.RU, Locale.JP, Locale.ZH, Locale.KR },
				{ Locale.ZH, Locale.JP, Locale.EN, Locale.KR },
				{ Locale.KR, Locale.JP, Locale.EN, Locale.ZH},
				{ Locale.JP, Locale.EN, Locale.ZH, Locale.KR },
				{ Locale.RU, Locale.EN, Locale.JP, Locale.ZH, Locale.KR },
				{ Locale.DE, Locale.EN, Locale.JP, Locale.ZH, Locale.KR },
				{ Locale.FR, Locale.EN, Locale.JP, Locale.ZH, Locale.KR },
				{ Locale.ES, Locale.EN, Locale.JP, Locale.ZH, Locale.KR },
				{ Locale.IT, Locale.EN, Locale.JP, Locale.ZH, Locale.KR },
				{ Locale.TH, Locale.EN, Locale.JP, Locale.ZH, Locale.KR }
		};

		/**
		 * PONOS由来データで対応する言語の一覧。
		 */
		@StaticPermitted
		public static final CommonStatic.Lang.Locale[] supportedLanguage = {
				CommonStatic.Lang.Locale.EN,
				CommonStatic.Lang.Locale.ZH,
				CommonStatic.Lang.Locale.JP,
				CommonStatic.Lang.Locale.KR,
				CommonStatic.Lang.Locale.DE,
				CommonStatic.Lang.Locale.FR,
				CommonStatic.Lang.Locale.ES,
				CommonStatic.Lang.Locale.IT,
				CommonStatic.Lang.Locale.TH
		};
	}

	/**
	 * 保存・音声処理を担う環境実装。アプリケーション初期化時に設定する。
	 */
	@StaticPermitted(StaticPermitted.Type.ENV)
	public static Itf def;

	/**
	 * ファイル解決とエラー通知を担う環境実装。アプリケーション初期化時に設定する。
	 */
	@StaticPermitted(StaticPermitted.Type.ENV)
	public static Context ctx;

	@StaticPermitted(StaticPermitted.Type.FINAL)
	public static final BigInteger max = new BigInteger(String.valueOf(Integer.MAX_VALUE));
	@StaticPermitted(StaticPermitted.Type.FINAL)
	public static final BigDecimal maxdbl = new BigDecimal(String.valueOf(Double.MAX_VALUE));
	@StaticPermitted(StaticPermitted.Type.FINAL)
	public static final BigInteger min = new BigInteger(String.valueOf(Integer.MIN_VALUE));
	@StaticPermitted(StaticPermitted.Type.FINAL)
	public static final BigDecimal mindbl = new BigDecimal(String.valueOf(Double.MIN_VALUE));

	public static BCAuxAssets getBCAssets() {
		return UserProfile.getStatic("BCAuxAssets", BCAuxAssets::new);
	}

	public static Config getConfig() {
		return UserProfile.getStatic("config", Config::new);
	}

	public static boolean isInteger(String str) {
		str = str.trim();

		if(str.isEmpty())
			return false;

		for (int i = 0; i < str.length(); i++) {
			if (!Character.isDigit(str.charAt(i))) {
				if(str.charAt(i) != '-' || i != 0)
					return false;
			}
		}

		return true;
	}

	public static boolean isDouble(String str) {
		int dots = 0;
		for (int i = 0; i < str.length(); i++) {
			if (!Character.isDigit(str.charAt(i))) {
				if((i == 0 && str.charAt(i) != '-') || str.charAt(i) != '.' || dots > 0)
					return false;
				else
					dots++;
			}
		}

		return true;
	}

	public static int parseIntN(String str) {
		int ans;
		try {
			ans = parseIntsN(str)[0];
		} catch (Exception e) {
			ans = -1;
		}
		return ans;
	}

	public static String verifyFileName(String str) {
		return str.replaceAll("[\\\\/:*<>?\"|]|\\.+$", "_");
	}

	public static double parseDoubleN(String str) {
		double ans;
		try {
			ans = parseDoublesN(str)[0];
		} catch (Exception e) {
			ans = -1.0;
		}
		return ans;
	}

	public static float parseFloatN(String str) {
		float ans;

		try {
			ans = parseFloatsN(str)[0];
		} catch (Exception e) {
			ans = -1f;
		}

		return ans;
	}

	public static double[] parseDoublesN(String str) {
		ArrayList<String> lstr = new ArrayList<>();
		Matcher matcher = Pattern.compile("-?(([.|,]\\d+)|\\d+([.|,]\\d*)?)").matcher(str);

		while (matcher.find())
			lstr.add(matcher.group());

		double[] result = new double[lstr.size()];
		for (int i = 0; i < lstr.size(); i++)
			result[i] = safeParseDouble(lstr.get(i));
		return result;
	}

	public static float[] parseFloatsN(String str) {
		ArrayList<String> lstr = new ArrayList<>();
		Matcher matcher = Pattern.compile("-?(([.|,]\\d+)|\\d+([.|,]\\d*)?)").matcher(str);

		while (matcher.find())
			lstr.add(matcher.group());

		float[] result = new float[lstr.size()];

		for (int i = 0; i < lstr.size(); i++)
			result[i] = safeParseFloat(lstr.get(i));

		return result;
	}

	public static int[] parseIntsN(String str) {
		ArrayList<String> lstr = new ArrayList<>();
		int t = -1;
		for (int i = 0; i < str.length(); i++)
			if (t == -1) {
				if (isDigit(str.charAt(i)) || str.charAt(i) == '-')
					t = i;
			} else if (!isDigit(str.charAt(i))) {
				lstr.add(str.substring(t, i));
				t = -1;
			}
		if (t != -1)
			lstr.add(str.substring(t));
		int ind = 0;
		while (ind < lstr.size()) {
			if (isDigit(lstr.get(ind).charAt(0)) || lstr.get(ind).length() > 1)
				ind++;
			else
				lstr.remove(ind);
		}
		int[] ans = new int[lstr.size()];
		for (int i = 0; i < lstr.size(); i++)
			ans[i] = safeParseInt(lstr.get(i));
		return ans;
	}

	public static int safeParseInt(String v) {
		if(isInteger(v)) {
			BigInteger big = new BigInteger(v);

			if(big.compareTo(max) > 0) {
				return Integer.MAX_VALUE;
			} else if(big.compareTo(min) < 0) {
				return Integer.MIN_VALUE;
			} else {
				return Integer.parseInt(v);
			}
		} else {
			throw new IllegalStateException("Value "+v+" isn't a number");
		}
	}

	public static double safeParseDouble(String v) {
		if(isDouble(v)) {
			BigDecimal big = new BigDecimal(v);

			if(big.compareTo(maxdbl) > 0) {
				return Double.MAX_VALUE;
			} else if(big.compareTo(mindbl) < 0) {
				return Double.MIN_VALUE;
			} else {
				return Double.parseDouble(v);
			}
		} else {
			throw new IllegalStateException("Value "+v+" isn't a number");
		}
	}

	public static float safeParseFloat(String v) {
		if(isDouble(v)) {
			BigDecimal big = new BigDecimal(v);

			if(big.compareTo(maxdbl) > 0) {
				return Float.MAX_VALUE;
			} else if(big.compareTo(mindbl) < 0) {
				return Float.MIN_VALUE;
			} else {
				return Float.parseFloat(v);
			}
		} else {
			throw new IllegalStateException("Value "+v+" isn't a number");
		}
	}

	public static String[] getPackContentID(String input) {
		StringBuilder packID = new StringBuilder();
		StringBuilder entityID = new StringBuilder();

		boolean packEnd = false;

		for (int i = 0; i < input.length(); i++) {
			if (!packEnd) {
				if (Character.toString(input.charAt(i)).matches("[0-9a-z]"))
					packID.append(input.charAt(i));
				else {
					packEnd = true;
				}
			} else {
				if (Character.isDigit(input.charAt(i)))
					entityID.append(input.charAt(i));
			}
		}

		return new String[] { packID.toString(), entityID.toString() };
	}

	public static String[] getPackEntityID(String input) {
		String[] result = new String[2];

		StringBuilder packID = new StringBuilder();
		StringBuilder entityID = new StringBuilder();

		boolean packEnd = false;
		boolean findDigit = false;

		for(int i = 0; i < input.length(); i++) {
			if(!packEnd) {
				if(Character.toString(input.charAt(i)).matches("[0-9a-z]"))
					packID.append(input.charAt(i));
				else {
					packEnd = true;
					findDigit = true;
				}
			} else {
				if(findDigit) {
					if(Character.isDigit(input.charAt(i))) {
						entityID.append(input.charAt(i));
						findDigit = false;
					}
				} else {
					if(Character.isDigit(input.charAt(i))) {
						entityID.append(input.charAt(i));
					} else if(input.charAt(i) == 'r') {
						entityID.append(input.charAt(i));
						break;
					} else {
						break;
					}
				}
			}
		}

		result[0] = packID.toString();
		result[1] = entityID.toString();

		return result;
	}

	public static long parseLongN(String str) {
		long ans;
		try {
			ans = parseLongsN(str)[0];
		} catch (Exception e) {
			ans = -1;
		}
		return ans;
	}

	public static long[] parseLongsN(String str) {
		ArrayList<String> lstr = new ArrayList<>();
		int t = -1;
		for (int i = 0; i < str.length(); i++)
			if (t == -1) {
				if (isDigit(str.charAt(i)) || str.charAt(i) == '-' || str.charAt(i) == '+')
					t = i;
			} else if (!isDigit(str.charAt(i))) {
				lstr.add(str.substring(t, i));
				t = -1;
			}
		if (t != -1)
			lstr.add(str.substring(t));
		int ind = 0;
		while (ind < lstr.size()) {
			if (isDigit(lstr.get(ind).charAt(0)) || lstr.get(ind).length() > 1)
				ind++;
			else
				lstr.remove(ind);
		}
		long[] ans = new long[lstr.size()];
		for (int i = 0; i < lstr.size(); i++)
			ans[i] = Long.parseLong(lstr.get(i));
		return ans;
	}

	/**
	 * 効果音を再生する。
	 */
	public static void setSE(int ind) {
		def.setSE(ind);
	}

	/**
	 * 識別子で指定した効果音を再生する。
	 */
	public static void setSE(Identifier<Music> mus) {
		def.setSE(mus);
	}

	/**
	 * BGMを再生する。
	 *
	 * @param music 音楽の識別子
	 */
	public static void setBGM(Identifier<Music> music) {
		def.setBGM(music);
	}

	public static String toArrayFormat(int... data) {
		StringBuilder res = new StringBuilder("{");

		for (int i = 0; i < data.length; i++) {
			if (i == data.length - 1) {
				res.append(data[i]).append("}");
			} else {
				res.append(data[i]).append(", ");
			}
		}

		return res.toString();
	}

	/**
	 * 標準敵モデルから配置可能な最小位置を算出する。
	 */
	public static float dataEnemyMinPos(MaModel model) {
		int x = ((model.confs[0][2] - model.parts[0][6]) * model.parts[0][8]) / model.ints[0];
		return 2.5f * x;
	}

	/**
	 * カスタム敵モデルから配置可能な最小位置を算出する。
	 */
	public static float customEnemyMinPos(MaModel model) {
		int x = ((model.confs[0][2] - model.parts[0][6]) * model.parts[0][8]) / model.ints[0];
		return 2.5f * x;
	}

	/**
	 * 標準味方モデルから配置可能な最小位置を算出する。
	 */
	public static int dataFormMinPos(MaModel model) {
		int x = ((model.confs[1][2] - model.parts[0][6]) * model.parts[0][8]) / model.ints[0];
		return 5 * x;
	}

	/**
	 * カスタム味方モデルから配置可能な最小位置を算出する。
	 */
	public static int customFormMinPos(MaModel model) {
		int x = (-model.parts[0][6] * model.parts[0][8]) / model.ints[0];
		return 5 * x;
	}

	/**
	 * 城画像のパラメーターからボス出現位置を算出する。
	 * 計算式の提供者は Domimmo314。
	 */
	public static float bossSpawnPoint(int x, int s) {
		int v = 3200 + x * s / 10 - s * 1180 / 100 + s * 127 / 10;
		return v / 4f;
	}
}
