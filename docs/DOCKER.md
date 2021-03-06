# Docker

First, install [Docker](https://www.docker.com/).

To run the application you'll need the following containers:

* MongoDB
* Stonecutter
* Nginx

## MongoDB container

To start a Mongo container, run 

    docker run -d -v /data/db --name mongo mongo
    
## Stonecutter container

To run Stonecutter you need 
 
* a clients.yml file 
* an rsa-keypair.json 
* a stonecutter.env file

To make a clients.yml file, copy the example in the config directory and add the details of the clients you want to use Stonecutter with. Set the client-id and client-secret to secure alphanumeric strings.

To get an rsa keypair, see [below](#adding-public-private-keypair-for-openid-connect).

Store both of these two files in their own directory.

To get a stonecutter.env, copy the template *config/stonecutter.env* and fill in your configuration. More information about the configuration variables can be found [here](./CONFIG.md).
 
Finally, run this command, replacing <config file path> with the absolute path for the directory storing your config files, and <env file path> with the path to wherever your environment variable file is stored.  

    docker run -v <config file path>:/var/config --env-file=<env file path> -v <favicon and logo absolute path directory>:/data/stonecutter/static -v <email service directory absolute path>:/var/stonecutter/email_service -p 5000:5000 --link mongo:mongo -d --restart=on-failure --name stonecutter dcent/stonecutter
    
An example script for deploying, deploy_snap.sh, is included in the ops directory.
    
    
## Starting an Nginx container

To start an Nginx container you need 

* an SSL certificate and key
* a dhparam.pem file
* an nginx.conf file

You can acquire an SSL certificate and key online inexpensively. You should receive a pair of files, for instance stonecutter.crt and stonecutter.key. Store them in their own directory somewhere safe.

You can generate a dhparam.pem file by running: 
    
    openssl dhparam -rand – 2048 > dhparam.pem
 
You can create an nginx.conf file by copying the following into a new file and replacing the <> appropriately:

    events {
    }
    http {
      server {
        listen 80;
        server_name <web address for site>;
        return 301 https://$server_name$request_uri;
      }
      server {
        listen 443 ssl;
        server_name <web address for site>;
        ssl_certificate /etc/nginx/ssl/<file name for SSL certificate>;
        ssl_certificate_key /etc/nginx/ssl/<file name for SSL key>;
    
        ssl_session_cache shared:SSL:32m;
        ssl_session_timeout 10m;
    
        ssl_dhparam /etc/nginx/cert/dhparam.pem;
        ssl_protocols TLSv1.2 TLSv1.1 TLSv1;
    
        location / {
          proxy_pass http://stonecutter:5000;
          proxy_set_header Host $host;
          proxy_set_header X-Real-IP $remote_addr;
          proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
          proxy_set_header X-Forwarded-Proto $scheme;
        }
      }
    }

Finally, run the following command:

    docker run -v <absolute path to SSL certificates and keys directory>:/etc/nginx/ssl -v <absolute path to conf file>/nginx.conf:/etc/nginx/nginx.conf -v <absolute path to dhparam file>/dhparam.pem:/etc/nginx/cert/dhparam.pem -p 443:443 -p 80:80 --link stonecutter:stonecutter -d --name nginx-container nginx
        
# Adding public-private keypair for OpenID Connect

To generate a public-private keypair in Json Web-key (JWK) format, you will first need to install [Leiningen](https://leiningen.org/) or start up the development VM using the instructions [here](https://github.com/d-cent/stonecutter#development-vm).

Next, navigate to the project directory and enter the following at the command line:

```
> lein gen-keypair <key-id>
```

where the key-id is a custom identifier for the key (for example "20150824-stonecutter").

This will generate output similar to the following:

    JWK public key for client:
    ==========================
    {"kty":"RSA","kid":"20150824-stonecutter","n":"m38hDunIOBX4DdalnuoNoT7nVdn5gXprVFUlBX3KbDIwyKznX2QZQLDn_4_b94UsYlh1Vf33pO9TO9tsj2Hf1WdFQO72WqFUxFOk3ITc7OTc7p5oZhWYXsKCJh5dLl9G4tOxZ_vD-frD7c0M_-IUWQ9cuk7XulDNJqzHKSEHvbugokw-vOb9fI2CtBU9HtWHkbe3e8cJdbEN4zD7Qw7BrG5zCENGuWIMpe9XIpZTM0jwiclxNacNhU_eOiRk9wg7hHovGqFuSU8x0oohtaNe91YUCJsfnmHQZTARc8tGJOwhx4A8VAUnqVmmm7GCGx0CqvbzRtFolTbn39m3jMTtoQ","e":"AQAB"}


    JWK including private key for stonecutter:
    ==========================================
    {"kty":"RSA","kid":"20150824-stonecutter","n":"m38hDunIOBX4DdalnuoNoT7nVdn5gXprVFUlBX3KbDIwyKznX2QZQLDn_4_b94UsYlh1Vf33pO9TO9tsj2Hf1WdFQO72WqFUxFOk3ITc7OTc7p5oZhWYXsKCJh5dLl9G4tOxZ_vD-frD7c0M_-IUWQ9cuk7XulDNJqzHKSEHvbugokw-vOb9fI2CtBU9HtWHkbe3e8cJdbEN4zD7Qw7BrG5zCENGuWIMpe9XIpZTM0jwiclxNacNhU_eOiRk9wg7hHovGqFuSU8x0oohtaNe91YUCJsfnmHQZTARc8tGJOwhx4A8VAUnqVmmm7GCGx0CqvbzRtFolTbn39m3jMTtoQ","e":"AQAB","d":"kRSlaH-xorrErUy3TLU-MFM7jnuI80igOZgTqbL7GcYehC3m1rbTZOtqGqVD7AaiKcQ0_h2uYII3m6KYAJOmPztSf0o2KstaBq-wI1wHsTO7-xtrdsvxVYCP5DbyY-Dbh6lSXh2mdWeGRSrLVTfAGnRd5SrI1vqq3snYLMS3r0qSubpVjo1yGjcOitxgJWgvdRq2FRPplgRlnoaiMd5jVCNXvSP-2XXeIQq0nz_GLcqcjOI0hqPsEPFcdjtL9PdwXa7v3cmrjOcWprlFzBQVTL6YvT_kCKIghJsG9ksJoUzTafHUAYUBdfgQSTi0q-kommHr3SyQhL1aN4Khqm3jLQ","p":"9wOYB-B7mhbGsxh7qago75DqUhp3L2x56yP1pYA2dV0TBNQz2jlGjAJ-xzMCQ-AMOpGNtzWJ28A-aDcUo1ZXIam3qktCha38fIAuvgKR7k0tnjhLawIONBaA-OlSorszlAWdHJ3_4ckn0c_u9Zne0SHkQESJNY7ES23-Sca3AL8","q":"oSc_HO3y61wgMUDDTMtMFYaJA9UdO4fIEfEyu46VvgvIN2kvf2ayHTb01Pk-XsoL2OJUcmjg4g19sBt8xGCRU8as4DOBHb22rbYQ7qTa4ewTtqLQBTnrTMzWZLYN2JYCZydFCW63z9zypC34Uoi_AF-teDprNY-eepRkr9JbSZ8","dp":"KDhGlenAVmuk-N5grFQ8Lh3LeYjjpS4lf9sAEW2Z8GwyP5QJyVuQGBYD7I1qrgCaHSM8DvvBsa1QvAlT6_CQCWQoCqtsbnXQ6bi5Y6jpeALLDbse1JKmG2caouzizqpqkIyFc3ZqhqoJOMmBoC3osOay0qAWM0lGvv1u7TZU7-M","dq":"C8uwnfB40Gts284OvYc_6W9whfxKaHoW1eFewkW8hi2cmRm05VFiBitonlIkE5IcbeKbJcixdTphkcthRYp_-K7ZJov-jmu9fFeQQ7eDYfgCtWKTcV5876EqrDJ7LvhD8sL4FamqAKf-hq_qtjfWKzPVobA8-q2pfvVvrULrdac","qi":"mAXOTpZF54XbnUQj3vVy5oFh2HtVyXZuCuTvDELKt6Z4x74xUBU7KCm_mq-tYEb_XWy_3trkQ-stP4RRAGwqLmFprxCX-G2uJOCBK6vpVsfDPUhSDe3CVEfyWVWu2knritBBhJX4dG-8I_cjFgCBFNz46Y9WG_5CdqkmshlpVDI"}


The first JSON document needs to be provided to any clients wishing to use OpenID Connect when interacting with Stonecutter, while the second should be stored in a file --- for example "rsa-keypair.json" --- which should be kept secure.

To deploy Stonecutter with this key, the rsa-keypair.json file needs to be placed in a directory accessible by the deployed instance, and the Stonecutter instance should be started with the environment variable ```RSA_KEYPAIR_FILE_PATH``` pointing to its location.

If deploying using the Snap CI tool (https://snap-ci.com/), this process is automated by including the rsa-keypair.json file in the secure files for the snap stage responsible for deployment, with the filename "rsa-keypair_<SNAP_STAGE_NAME>.json".  The ```ops/deploy_snap.sh``` script will then manage copying the file to the appropriate location on the target, and starting the app with the environment variable set.
