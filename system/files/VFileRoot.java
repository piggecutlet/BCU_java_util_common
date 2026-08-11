package common.system.files;

/**
 * {@code ./org/...} 形式の仮想パスを起点に、仮想ファイルツリーを構築・検索するルートノード。
 */
public class VFileRoot extends VFile {

	public VFileRoot(String str) {
		super(str);
	}

	/**
	 * ルート名に相当する先頭要素を除いてパスをたどり、不足するディレクトリを作成する。
	 * 終端が既存ノードの場合、{@code fd} が非nullならそのデータを置き換える。
	 */
	public VFile build(String str, FileData fd) {
		String[] strs = str.split("/|\\\\");
		VFile par = this;
		for (int i = 1; i < strs.length; i++) {
			VFile next = null;
			for (VFile ch : par.list())
				if (ch.name.equals(strs[i]))
					next = ch;
			if (next == null)
				if (i == strs.length - 1)
					if (fd != null)
						return new VFile(par, strs[i], fd);
					else
						return new VFile(par, strs[i]);
				else
					next = new VFile(par, strs[i]);
			if (i == strs.length - 1) {
				if (fd == null)
					return next;
				next.setData(fd);
				return next;
			}
			par = next;
		}
		return null;
	}

	/**
	 * ルート名に相当する先頭要素を除いてパスをたどる。
	 *
	 * @return 対応するノード。途中の要素が存在しない場合は{@code null}
	 */
	public VFile find(String str) {
		String[] strs = str.split("/|\\\\");
		VFile par = this;
		for (int i = 1; i < strs.length; i++) {
			VFile next = null;
			for (VFile ch : par.list())
				if (ch.name.equals(strs[i]))
					next = ch;
			if (next == null)
				return null;
			if (i == strs.length - 1)
				return next;
			par = next;
		}
		return this;
	}
}
