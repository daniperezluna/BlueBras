package com.bluebas;


import android.app.Activity;
import android.app.AlertDialog;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.Toast;
//import android.widget.EditText;

public class Main extends Activity {
	
	private static final int REQUEST_CONNECT_DEVICE = 1;

	public static final int MESSAGE_STATE_CHANGE = 1;

	public static final int MESSAGE_DEVICE_NAME = 4;

	public static final String DEVICE_NAME = "device_name";

	public static final int MESSAGE_TOAST = 5;

	public static final String TOAST = "toast";

	public static final int MESSAGE_READ = 2;
	
	public static final int MESSAGE_WRITE = 3;

	
	 public static final int STATE_NONE = 0;     
	
	 private static BluetoothCommandService mSerialService = null;
	    
	
	 public static final boolean DEBUG = true;
	 public static final String LOG_TAG = "bluebas";

	 private BluetoothAdapter mBluetoothAdapter = null;
	 private static final int REQUEST_ENABLE_BT = 2;
	 private boolean mEnablingBT;
	 //private boolean mLocalEcho = false;
	 
	// private static InputMethodManager mInputManager;
	 private String mConnectedDeviceName = null;
	 
	 //relacionado con menuItem
	 private MenuItem mMenuItemConnect;
	
	// private static TextView mTitle;
	
	 
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		if (DEBUG)Log.e(LOG_TAG, "+++ ON CREATE +++");//envia mensage +++ ON CREATE +++ a la ventana LogCat para debugear el programa
		
	
		setContentView(R.layout.main);//selecciona el form principal de nuestra aplicacion
	
		final Button button1 = (Button) findViewById(R.id.button1);        
		mSerialService = new BluetoothCommandService(this, mHandlerBT);//imprescindible para realizar conexion bluetooth
		
		button1.setOnClickListener(new OnClickListener() {
			public void onClick(View v) {
				mSerialService.write(10);
			
			}
			});
		
		mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
		
		if (mBluetoothAdapter == null) {//Comprueba si el adaptador bluetooth esta activado.
            finishDialogNoBluetooth();// en caso contrario aparecera un cuadro de dialogo indicandolo
			return;
		}
		
		mSerialService = new BluetoothCommandService(this, mHandlerBT);       
		
		if (DEBUG)
			Log.e(LOG_TAG, "+++ DONE IN ON CREATE +++");
		
	}
	
	 public void onStart() {
			super.onStart();
			if (DEBUG)
				Log.e(LOG_TAG, "++ ON START ++"+ getConnectionState());
			
			mEnablingBT = false;
		}
	
	public synchronized void onResume() {
		super.onResume();

		if (DEBUG) {
			Log.e(LOG_TAG, "+ ON RESUME +");
		}
		
		// Si el bluetooth del dispositivo esta desactivado,visualiza un dialogo para activar el bluetooth
		// y si no lo activamos,salir de la aplicacion.
		
		if (!mEnablingBT) { // If we are turning on the BT we cannot check if it's enable
		    if ( (mBluetoothAdapter != null)  && (!mBluetoothAdapter.isEnabled()) ) {
			
                AlertDialog.Builder builder = new AlertDialog.Builder(this);
                builder.setMessage(R.string.alert_dialog_turn_on_bt)
                    .setIcon(android.R.drawable.ic_dialog_alert)
                    .setTitle(R.string.alert_dialog_warning_title)
                    .setCancelable( false )
                    .setPositiveButton(R.string.alert_dialog_yes, new DialogInterface.OnClickListener() {
                    	public void onClick(DialogInterface dialog, int id) {
                    		mEnablingBT = true;
                    		Intent enableIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
                    		startActivityForResult(enableIntent, REQUEST_ENABLE_BT);			
                    	}
                    })
                    .setNegativeButton(R.string.alert_dialog_no, new DialogInterface.OnClickListener() {
                    	public void onClick(DialogInterface dialog, int id) {
                    		finishDialogNoBluetooth();            	
                    	}
                    });
                AlertDialog alert = builder.create();
                alert.show();
		    }		
		
		    if (mSerialService != null) {
		    	// Only if the state is STATE_NONE, do we know that we haven't started already
		    	if (mSerialService.getState() == BluetoothCommandService.STATE_NONE) {
		    		// Start the Bluetooth  services
		    		
		    		
		    		mSerialService.start();
		    	}
		    }

		 
		}
	}
	// Mensage relacionado con no haber activado el bluetooth del dispositivo.
	
	 public void finishDialogNoBluetooth() {
	        AlertDialog.Builder builder = new AlertDialog.Builder(this);
	        builder.setMessage(R.string.alert_dialog_no_bt)
	        .setIcon(android.R.drawable.ic_dialog_info)
	        .setTitle(R.string.app_name)
	        .setCancelable( false )
	        .setPositiveButton(R.string.alert_dialog_ok, new DialogInterface.OnClickListener() {
	                   public void onClick(DialogInterface dialog, int id) {
	                       finish();            	
	                	   }
	               });
	        AlertDialog alert = builder.create();
	        alert.show(); 
	    }
	 
		public void onDestroy() {// Salir por completo de la aplicacion.por aqui se pasa cuando tocamos el boton salir de hardware de nuestro dispositivo
			super.onDestroy();
			if (DEBUG)
				Log.e(LOG_TAG, "--- ON DESTROY ---");
			
	        if (mSerialService != null)
	        	mSerialService.stop();
	        
		}
// Crea pestañas en el menu de opciones.//  src/res/menu
	@Override
	 public boolean onCreateOptionsMenu(Menu menu) {
		Log.e(LOG_TAG, "menu "+ getConnectionState());
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.main, menu);
        mMenuItemConnect = menu.getItem(0);
        Log.e(LOG_TAG, "menu2 "+ getConnectionState());
        return true;
    }

	// Acceso al hacer clic en las pestañas
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
    	Log.e(LOG_TAG, "itemSelected"+ getConnectionState());
        switch (item.getItemId()) {
        case R.id.connect:
        	Log.e(LOG_TAG, "connect");
        	if (getConnectionState() == BluetoothCommandService.STATE_NONE) {
        		// Launch the DeviceListActivity to see devices and do scan--identificarlo en el manifest para que funcione.
        		Intent serverIntent = new Intent(this, DeviceListActivity.class);
        		startActivityForResult(serverIntent, REQUEST_CONNECT_DEVICE);
        		Log.e(LOG_TAG, "menu in");
        	}
        	else
        		// Ejecucion la seleccionar Desconectar en el menu de opciones
            	if (getConnectionState() == BluetoothCommandService.STATE_CONNECTED) {
            		mSerialService.stop();
		    		mSerialService.start();
		    		Log.e(LOG_TAG, "menu out" );
		    		
            	}
            return true;
        }
        return false;
}
	
    public int getConnectionState() {
    	Log.e(LOG_TAG, "getConnectionState "+ mSerialService.getState());
		return mSerialService.getState();
    	
  }
	
   // Este Handler  recibe información desde el BluetoothService para informar sobre el estado de la conexion
    private final Handler mHandlerBT = new Handler() {
    	
          public void handleMessage(Message msg) {        	
            switch (msg.what) {
            case MESSAGE_STATE_CHANGE:
                if(DEBUG) Log.e(LOG_TAG, "MESSAGE_STATE_CHANGE: " + msg.arg1);
                switch (msg.arg1) {
                case BluetoothCommandService.STATE_CONNECTED:
                	if(DEBUG) Log.e(LOG_TAG, "STATE_CONNECTED" + msg.arg1);
                	if (mMenuItemConnect != null) {
                		
                		mMenuItemConnect.setTitle(R.string.disconnect);// muestra "desconectar" en el menu de opciones
                	}
                	
              
                    break;
                    
                case BluetoothCommandService.STATE_CONNECTING:
                	if(DEBUG) Log.e(LOG_TAG, "STATE_CONNECTING: " + msg.arg1);
                   // mTitle.setText(R.string.title_connecting);
                	 Toast.makeText(getApplicationContext(), "Conectando..." , Toast.LENGTH_LONG).show();
                    break;
                    
                case BluetoothCommandService.STATE_LISTEN:// En caso de salir del menu.parar la conexion.
                	if(DEBUG) Log.e(LOG_TAG, "STATE_LISTEN" + msg.arg1);
                	
                	mSerialService.stop();
                	break;
                case BluetoothCommandService.STATE_NONE:
                	if(DEBUG) Log.e(LOG_TAG, "STATE_NONE" + msg.arg1);
                	if (mMenuItemConnect != null) {
                	
                		mMenuItemConnect.setTitle(R.string.connect);// Muestra "Conectar" en el menu de opciones
                		
                	}
                	
            
                    break;
                }
                break;
        
            case MESSAGE_DEVICE_NAME:
                // save the connected device's name
                mConnectedDeviceName = msg.getData().getString(DEVICE_NAME);
                Toast.makeText(getApplicationContext(), "Conectado a: " + mConnectedDeviceName, Toast.LENGTH_SHORT).show();
                break;
          
            case MESSAGE_TOAST:
                Toast.makeText(getApplicationContext(), msg.getData().getString(TOAST),Toast.LENGTH_SHORT).show();//mensage de "conexion perdida"
                break;
                
            }
        }
    };
       
        
        // Al seleccionar el dispositivo a conectar del dispositivo conecta a este.
        public void onActivityResult(int requestCode, int resultCode, Intent data) {
            if(DEBUG) Log.e(LOG_TAG, "onActivityResult " + resultCode);
            switch (requestCode) {
            
            case REQUEST_CONNECT_DEVICE:
            	 if(DEBUG) Log.e(LOG_TAG, "REQUEST_CONNECT_DEVICE" );

                // When DeviceListActivity returns with a device to connect
                if (resultCode == Activity.RESULT_OK) {
                    // Get the device MAC address
                    String address = data.getExtras()
                                         .getString(DeviceListActivity.EXTRA_DEVICE_ADDRESS);
                    // Get the BLuetoothDevice object
                    BluetoothDevice device = mBluetoothAdapter.getRemoteDevice(address);
                    // Attempt to connect to the device
                    mSerialService.connect(device);                
                }
                break;

            }
        }
    };    
    
    
    
