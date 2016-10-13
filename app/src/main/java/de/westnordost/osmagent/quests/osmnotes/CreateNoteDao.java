package de.westnordost.osmagent.quests.osmnotes;

import android.content.ContentValues;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import java.util.ArrayList;
import java.util.List;

import javax.inject.Inject;

import de.westnordost.osmagent.quests.WhereSelectionBuilder;
import de.westnordost.osmapi.map.data.BoundingBox;
import de.westnordost.osmapi.map.data.OsmLatLon;

public class CreateNoteDao
{
	protected final SQLiteOpenHelper dbHelper;

	@Inject public CreateNoteDao(SQLiteOpenHelper dbHelper)
	{
		this.dbHelper = dbHelper;
	}

	public boolean add(CreateNote note)
	{
		SQLiteDatabase db = dbHelper.getWritableDatabase();
		ContentValues values = new ContentValues();
		values.put(CreateNoteTable.Columns.LATITUDE, note.position.getLatitude());
		values.put(CreateNoteTable.Columns.LONGITUDE, note.position.getLongitude());
		values.put(CreateNoteTable.Columns.TEXT, note.text);

		long rowId = db.insert(CreateNoteTable.NAME, null, values);

		if(rowId != -1)
		{
			note.id = rowId;
			return true;
		}
		return false;
	}

	public CreateNote get(long id)
	{
		SQLiteDatabase db = dbHelper.getWritableDatabase();
		Cursor cursor = db.query(CreateNoteTable.NAME, null, CreateNoteTable.Columns.ID + " = " + id,
				null, null, null, null, "1");

		try
		{
			if(!cursor.moveToFirst()) return null;
			return createObjectFrom(cursor);
		}
		finally
		{
			cursor.close();
		}
	}

	public boolean delete(long id)
	{
		SQLiteDatabase db = dbHelper.getWritableDatabase();
		return db.delete(CreateNoteTable.NAME, CreateNoteTable.Columns.ID + " = " + id, null) == 1;
	}

	public List<CreateNote> getAll(BoundingBox bbox)
	{
		SQLiteDatabase db = dbHelper.getReadableDatabase();

		WhereSelectionBuilder builder = new WhereSelectionBuilder();
		if(bbox != null)
		{
			builder.appendAnd("(" + CreateNoteTable.Columns.LATITUDE + " BETWEEN ? AND ?)",
					String.valueOf(bbox.getMinLatitude()),
					String.valueOf(bbox.getMaxLatitude()));
			builder.appendAnd("(" + CreateNoteTable.Columns.LONGITUDE + " BETWEEN ? AND ?)",
					String.valueOf(bbox.getMinLongitude()),
					String.valueOf(bbox.getMaxLongitude()));
		}

		Cursor cursor = db.query(CreateNoteTable.NAME, null, builder.getWhere(), builder.getArgs(),
				null, null, null, null);

		List<CreateNote> result = new ArrayList<>();

		try
		{
			if(cursor.moveToFirst())
			{
				while(!cursor.isAfterLast())
				{
					result.add(createObjectFrom(cursor));
					cursor.moveToNext();
				}
			}
		}
		finally
		{
			cursor.close();
		}

		return result;
	}

	private CreateNote createObjectFrom(Cursor cursor)
	{
		int colNoteId = cursor.getColumnIndexOrThrow(CreateNoteTable.Columns.ID),
			colLat = cursor.getColumnIndexOrThrow(CreateNoteTable.Columns.LATITUDE),
			colLon = cursor.getColumnIndexOrThrow(CreateNoteTable.Columns.LONGITUDE),
			colText = cursor.getColumnIndexOrThrow(CreateNoteTable.Columns.TEXT);

		CreateNote note = new CreateNote();
		note.position = new OsmLatLon(cursor.getDouble(colLat), cursor.getDouble(colLon));
		note.text = cursor.getString(colText);
		note.id = cursor.getLong(colNoteId);

		return note;
	}

}
