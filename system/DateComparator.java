package common.system;

import common.io.Backup;

import java.util.Comparator;

/**
 * {@link Backup}をファイル名由来の日時が新しい順に並べる比較器。
 */
public class DateComparator implements Comparator<Backup> {
    @Override
    public int compare(Backup o1, Backup o2) {
        return Long.compare(Long.parseLong(o2.getName().replaceAll("[:\\-/]", "")), Long.parseLong(o1.getName().replaceAll("[:\\-/]", "")));
    }
}
