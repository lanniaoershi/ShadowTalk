/*
 * This file is auto-generated.  DO NOT MODIFY.
 * Original file: /home/weiwei/SemcApps/partyshare/src/com/sonymobile/partyshare/service/IMusicService.aidl
 */
package com.sonymobile.partyshare.service;
public interface IMusicService extends android.os.IInterface
{
/** Local-side IPC implementation stub class. */
public static abstract class Stub extends android.os.Binder implements com.sonymobile.partyshare.service.IMusicService
{
private static final java.lang.String DESCRIPTOR = "com.sonymobile.partyshare.service.IMusicService";
/** Construct the stub at attach it to the interface. */
public Stub()
{
this.attachInterface(this, DESCRIPTOR);
}
/**
 * Cast an IBinder object into an com.sonymobile.partyshare.service.IMusicService interface,
 * generating a proxy if needed.
 */
public static com.sonymobile.partyshare.service.IMusicService asInterface(android.os.IBinder obj)
{
if ((obj==null)) {
return null;
}
android.os.IInterface iin = obj.queryLocalInterface(DESCRIPTOR);
if (((iin!=null)&&(iin instanceof com.sonymobile.partyshare.service.IMusicService))) {
return ((com.sonymobile.partyshare.service.IMusicService)iin);
}
return new com.sonymobile.partyshare.service.IMusicService.Stub.Proxy(obj);
}
@Override public android.os.IBinder asBinder()
{
return this;
}
@Override public boolean onTransact(int code, android.os.Parcel data, android.os.Parcel reply, int flags) throws android.os.RemoteException
{
switch (code)
{
case INTERFACE_TRANSACTION:
{
reply.writeString(DESCRIPTOR);
return true;
}
case TRANSACTION_sendPrepared:
{
data.enforceInterface(DESCRIPTOR);
this.sendPrepared();
reply.writeNoException();
return true;
}
case TRANSACTION_sendnextMusic:
{
data.enforceInterface(DESCRIPTOR);
this.sendnextMusic();
reply.writeNoException();
return true;
}
case TRANSACTION_sendMusicInfo:
{
data.enforceInterface(DESCRIPTOR);
this.sendMusicInfo();
reply.writeNoException();
return true;
}
}
return super.onTransact(code, data, reply, flags);
}
private static class Proxy implements com.sonymobile.partyshare.service.IMusicService
{
private android.os.IBinder mRemote;
Proxy(android.os.IBinder remote)
{
mRemote = remote;
}
@Override public android.os.IBinder asBinder()
{
return mRemote;
}
public java.lang.String getInterfaceDescriptor()
{
return DESCRIPTOR;
}
@Override public void sendPrepared() throws android.os.RemoteException
{
android.os.Parcel _data = android.os.Parcel.obtain();
android.os.Parcel _reply = android.os.Parcel.obtain();
try {
_data.writeInterfaceToken(DESCRIPTOR);
mRemote.transact(Stub.TRANSACTION_sendPrepared, _data, _reply, 0);
_reply.readException();
}
finally {
_reply.recycle();
_data.recycle();
}
}
@Override public void sendnextMusic() throws android.os.RemoteException
{
android.os.Parcel _data = android.os.Parcel.obtain();
android.os.Parcel _reply = android.os.Parcel.obtain();
try {
_data.writeInterfaceToken(DESCRIPTOR);
mRemote.transact(Stub.TRANSACTION_sendnextMusic, _data, _reply, 0);
_reply.readException();
}
finally {
_reply.recycle();
_data.recycle();
}
}
@Override public void sendMusicInfo() throws android.os.RemoteException
{
android.os.Parcel _data = android.os.Parcel.obtain();
android.os.Parcel _reply = android.os.Parcel.obtain();
try {
_data.writeInterfaceToken(DESCRIPTOR);
mRemote.transact(Stub.TRANSACTION_sendMusicInfo, _data, _reply, 0);
_reply.readException();
}
finally {
_reply.recycle();
_data.recycle();
}
}
}
static final int TRANSACTION_sendPrepared = (android.os.IBinder.FIRST_CALL_TRANSACTION + 0);
static final int TRANSACTION_sendnextMusic = (android.os.IBinder.FIRST_CALL_TRANSACTION + 1);
static final int TRANSACTION_sendMusicInfo = (android.os.IBinder.FIRST_CALL_TRANSACTION + 2);
}
public void sendPrepared() throws android.os.RemoteException;
public void sendnextMusic() throws android.os.RemoteException;
public void sendMusicInfo() throws android.os.RemoteException;
}
