package pro.dbro.timelapse;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;

public class BrowserViewBinder implements android.widget.SimpleAdapter.ViewBinder {

	@Override
	public boolean setViewValue(View browser_list_item, Object data,
			String textRepresentation) {
		
		// If this is the list item body, tag it with the related timelapse id
		if( browser_list_item.getClass().toString().equals("class pro.dbro.timelapse.ListItemRelativeLayout")){
			browser_list_item.setTag(R.id.view_related_timelapse, Integer.parseInt(data.toString()));
			// ListView item ids = timelapse_id
			browser_list_item.setId(Integer.parseInt(data.toString()));
			return true; // do not ask SimpleAdapter superclass to do binding on this view
		}
		else if(browser_list_item.getId() == R.id.list_item_body){
			if(data.toString().compareTo("") == 0){
				//Log.d("Binder","setting body invisible");
				browser_list_item.setVisibility(View.GONE);
			}
		}
		else if(browser_list_item.getId() == R.id.list_item_image){
			Bitmap myBitmap = BitmapFactory.decodeFile(String.valueOf(data));

    	    ((ImageView)browser_list_item).setImageBitmap(myBitmap);
		}
		
		return false; // pass binding to SimpleAdapter
	}

}
