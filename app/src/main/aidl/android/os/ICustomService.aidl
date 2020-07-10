package android.os;

interface ICustomService {
    byte[] option(in String execType, in byte[] bytes);
}
