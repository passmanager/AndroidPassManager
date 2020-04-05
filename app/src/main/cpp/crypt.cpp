#include "crypt.h"

void crypt(std::string &str, std::string key, std::string salt){
	key = key + salt;
	for (int i = 0; i < 512; ++i){
		key = sha512(key + salt);
	}
  	while (key.length() < str.length()){
    	key += sha512(key + salt);
  	}
	for (unsigned long i = 0; i < str.length(); ++i){
		str[i] = str[i] ^ key[i];
	}
}
