package se.exjobb.feedloopt;

import android.support.annotation.NonNull;
import android.support.v4.app.FragmentTransaction;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.Toolbar;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import se.exjobb.feedloopt.fragments.LoginFragment;
import se.exjobb.feedloopt.fragments.RegisterFragment;

public class MainActivity extends AppCompatActivity implements
        LoginFragment.OnLoginListener,
        RegisterFragment.onRegisterListener{

    private Toolbar mToolbar;
    private DatabaseReference mDataRef;
    private FirebaseAuth mAuth;
    private FirebaseAuth.AuthStateListener mAuthStateListener;
    private OnCompleteListener mOnCompleteListener;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        toolbarSetup();

        mAuth = FirebaseAuth.getInstance();
        mDataRef = FirebaseDatabase.getInstance().getReference();
        mDataRef.keepSynced(true);

        // make the LoginFragment first page
        switchToLoginFragment();
        initializeListeners();

    }


    public void toolbarSetup(){
        mToolbar = (Toolbar) findViewById(R.id.toolbar);
        TextView mTitle = (TextView) mToolbar.findViewById(R.id.toolbar_title);
        setSupportActionBar(mToolbar);
        getSupportActionBar().setDisplayShowTitleEnabled(false);
        mTitle.setText("FeedLoop");
    }
    /************************ Firebase Auth ****************************/

    // Initialize listeners
    private void initializeListeners() {
        mAuthStateListener = new FirebaseAuth.AuthStateListener() {
            @Override
            public void onAuthStateChanged(@NonNull FirebaseAuth firebaseAuth) {
                FirebaseUser user = firebaseAuth.getCurrentUser();
                final String teacherUid = user.getUid();
                if (user != null) {
                    mDataRef.child("users/teachers/" + user.getUid() + "/name").addListenerForSingleValueEvent(new ValueEventListener() {
                        @Override
                        public void onDataChange(DataSnapshot dataSnapshot) {
                            String teacherName = dataSnapshot.getValue(String.class);
                            // Save current teachers name in SharedPreferences
                            SharedPreferencesUtils.setCurrentTeacherName(getApplicationContext(), teacherName);

                            // Save current teachers uid in SharedPreferences
                            SharedPreferencesUtils.setCurrentTeacherUid(getApplicationContext(), teacherUid);
                        }
                        @Override
                        public void onCancelled(DatabaseError databaseError) {

                        }
                    });
                    // if user exists go to CourseListFragment
                    Toast.makeText(MainActivity.this, "Login OK", Toast.LENGTH_SHORT).show();
                    //switchToCourseListFragment(teacherUid);
                } else {
                    switchToLoginFragment();
                }
            }
        };

        // if Login is not complete then show AlertDialog error
        mOnCompleteListener = new OnCompleteListener() {
            @Override
            public void onComplete(@NonNull Task task) {
                if (!task.isSuccessful()) {
                    showLoginError("Login Failed");
                }
                else{
                    Toast.makeText(MainActivity.this, "Login Successfull", Toast.LENGTH_SHORT).show();
                }

            }
        };
    }

    // Get the error message for failed login from LoginFragment
    private void showLoginError(String message) {
        LoginFragment loginFragment = (LoginFragment) getSupportFragmentManager().findFragmentByTag("Login");
        loginFragment.onLoginError(message);
    }


    // Check and connect user to a database when Login pressed in LoginFragment
    @Override
    public void onLogin(String email, String passwrod) {
        mAuth.signInWithEmailAndPassword(email, passwrod)
                .addOnCompleteListener(mOnCompleteListener);
    }

    // Sign out when user signs out
     public void onLogout() {
        mAuth.signOut();
    }

    /******************** Fragment Transactions ************************/

    private void switchToLoginFragment() {
        FragmentTransaction ft = getSupportFragmentManager().beginTransaction();
        ft.replace(R.id.fragment_container, new LoginFragment(), "Login");
        ft.commit();
    }



    @Override
    public void onRegisterClicked() {
        FragmentTransaction ft = getSupportFragmentManager().beginTransaction();
        RegisterFragment fragment = new RegisterFragment();
        ft.replace(R.id.fragment_container, fragment);
        ft.addToBackStack("back_to_login");
        ft.commit();
    }


    @Override
    public void onRegisterUserClicked(String name, String email, String password) {
        final String username = name;
        mAuth.createUserWithEmailAndPassword(email, password).addOnCompleteListener(new OnCompleteListener<AuthResult>() {
            @Override
            public void onComplete(@NonNull Task<AuthResult> task) {
                if (task.isSuccessful()) {
                    String userUid = mAuth.getCurrentUser().getUid();
                    DatabaseReference currentUserRef = mDataRef.child("users/teachers/" + userUid);
                    currentUserRef.child("name").setValue(username);
                    switchToLoginFragment();
                }
                else{
                    Toast.makeText(MainActivity.this, "Error", Toast.LENGTH_SHORT).show();
                }
            }
        });
    }
}


