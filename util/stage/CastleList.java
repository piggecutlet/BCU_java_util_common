package common.util.stage;

import common.io.json.JsonClass;
import common.io.json.JsonClass.RType;
import common.pack.FixIndexList.FixIndexMap;
import common.pack.IndexContainer;
import common.pack.PackData.UserPack;
import common.pack.UserProfile;
import common.system.VImg;
import common.system.files.VFile;
import common.util.stage.MapColc.PackMapColc;

import java.util.*;

/**
 * 城画像を供給元ごとに固定インデックスで管理する定義コンテナ。
 * 組み込み一覧とユーザーパック一覧を同じ文字列識別子のレジストリへ登録する。
 */
@JsonClass
public abstract class CastleList extends FixIndexMap<CastleImg> implements IndexContainer {

	/**
	 * 組み込み画像ディレクトリから構成される城一覧。
	 */
	public static class DefCasList extends CastleList {

		public final String str;
		public final String id;

		public DefCasList(String hash, String name) {
			id = hash;
			str = name;
			map().put(id, this);
			defset().add(this);
			for (VFile vf : VFile.get("./org/img/" + name).list()) {
				if (vf.getName().matches(name+"[0-9]+\\.png"))
					add(new CastleImg(getNextID(CastleImg.class), new VImg(vf)));
			}

		}

		@Override
		public String getSID() {
			return id;
		}

		@Override
		public String toString() {
			return str + " (" + size() + ")";
		}
	}

	/**
	 * ユーザーパックに所有される城一覧。
	 */
	@JsonClass(read = RType.FILL)
	public static class PackCasList extends CastleList {

		public final UserPack pack;

		public PackCasList(UserPack p) {
			pack = p;
			map().put(pack.desc.id, this);
		}

		@Override
		public String getSID() {
			return pack.getSID();
		}

		@Override
		public String toString() {
			return pack.desc.names.toString() + " (" + size() + ")";
		}
	}

	private static final String REG_CASTLE = "castle";

	private static final String REG_DEF_CASTLE = "def_castle";

	public static Set<CastleList> defset() {
		return UserProfile.getPool(REG_DEF_CASTLE, CastleList.class);
	}

	public static Collection<CastleList> from(Stage sta) {
		MapColc mc = sta.getCont().getCont();
		if (!(mc instanceof PackMapColc))
			return defset();
		List<CastleList> list = new ArrayList<>(defset());
		UserPack pack = ((PackMapColc) mc).pack;
		list.add(pack.castles);
		for (String rel : pack.desc.dependency)
			list.add(UserProfile.getUserPack(rel).castles);
		return list;
	}

	@ContGetter
	public static CastleList getList(String str) {
		return map().get(str);
	}

	public static Map<String, CastleList> map() {
		return UserProfile.getRegister(REG_CASTLE, CastleList.class);
	}

	private CastleList() {
		super(CastleImg.class);
	}

	@SuppressWarnings("rawtypes")
	@Override
	public <R> R getList(Class cls, Reductor<R, FixIndexMap> func, R def) {
		return func.reduce(def, this);
	}

}
