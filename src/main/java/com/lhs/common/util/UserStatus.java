package com.lhs.common.util;

public class UserStatus {


    public static int addPermission(int status,int permission){
        status = status|permission;
        return status;
    }

    public static int  disablePermission(int status,int permission){
        status = status&~permission;
        return status;
    }

    public static boolean hasPermission(int status,int permission) {
        return (status & permission) == permission;
    }

    public boolean NotHasPermission(int status,int permission){
        return (status&permission)==0;
    }
}
