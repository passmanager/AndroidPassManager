#include "crypt.h"

void crypt(std::string &str, std::string key, std::string salt){
	key = key + salt;
	for (int i = 0; i < 512; ++i){
		key = sha512(key + salt);
	}
	for (int i = 0, j = 0; i < str.length(); ++i, ++j){
		str[i] = str[i] ^ key[j % key.length()];
	}
}
