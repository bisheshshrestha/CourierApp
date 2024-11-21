package com.divyagyan.adminapp.Adapter;

import android.content.Context;
import android.content.Intent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.divyagyan.adminapp.R;
import com.divyagyan.adminapp.User;
import com.divyagyan.adminapp.UserDetailsActivity;
import com.google.firebase.database.FirebaseDatabase;
import java.util.List;

public class UserAdapter extends RecyclerView.Adapter<UserAdapter.UserViewHolder> {

    private final Context context;
    private final List<User> userList;

    public UserAdapter(Context context, List<User> userList) {
        this.context = context;
        this.userList = userList;
    }

    @NonNull
    @Override
    public UserViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_user, parent, false);
        return new UserViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull UserViewHolder holder, int position) {
        User user = userList.get(position);
        holder.textViewUserName.setText(user.getUserName());
        holder.textViewEmail.setText(user.getEmail());
        holder.textViewPhone.setText(user.getPhone());

        // Handle click event
        holder.itemView.setOnClickListener(v -> {
            Intent intent = new Intent(context, UserDetailsActivity.class);
            intent.putExtra("uid", user.getUid());
            intent.putExtra("userName", user.getUserName());
            intent.putExtra("email", user.getEmail());
            intent.putExtra("phone", user.getPhone());
            intent.putExtra("address", user.getAddress());
            context.startActivity(intent);
        });
    }

    @Override
    public int getItemCount() {
        return userList.size();
    }

    public static class UserViewHolder extends RecyclerView.ViewHolder {

        TextView textViewUserName, textViewEmail, textViewPhone;

        public UserViewHolder(@NonNull View itemView) {
            super(itemView);
            textViewUserName = itemView.findViewById(R.id.textViewUserName);
            textViewEmail = itemView.findViewById(R.id.textViewEmail);
            textViewPhone = itemView.findViewById(R.id.textViewPhone);
        }
    }
}
