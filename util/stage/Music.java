package common.util.stage;

import common.io.json.JsonClass;
import common.io.json.JsonField;
import common.pack.Identifier;
import common.pack.IndexContainer.IndexCont;
import common.pack.IndexContainer.Indexable;
import common.pack.PackData;
import common.system.files.FileData;
import common.util.Data;

import java.util.ArrayList;
import java.util.List;

/**
 * パック内の戦闘用楽曲を、識別子・ループ位置・音声データと対応付ける定義。
 * ステージ側は音声データを直接保持せず、この識別子を参照する。
 */
@JsonClass
@IndexCont(PackData.class)
@JsonClass.JCGeneric(Identifier.class)
public class Music implements Indexable<PackData, Music> {

	@JsonField
	@JsonClass.JCIdentifier
	public final Identifier<Music> id;
	@JsonField
	public long loop;

	public FileData data;

	@JsonClass.JCConstructor
	@Deprecated
	public Music() {
		id = null;
	}

	public Music(Identifier<Music> id, long loop, FileData fd) {
		this.id = id;
		this.loop = loop;
		data = fd;
	}

	@Override
	public Identifier<Music> getID() {
		return id;
	}

	@Override
	public String toString() {
		if (id != null) {
			return Data.trio(id.id) + ".ogg - " + id.pack;
		} else {
			return null;
		}
	}

	public List<Stage> getStages() {
		List<Stage> ans = new ArrayList<>();
		for (Stage st : MapColc.getAllStage()) {
			if (st != null && (st.mus0 != null && st.mus0.equals(id) || st.mus1 != null && st.mus1.equals(id)))
				ans.add(st);
		}
		return ans;
	}
}
