## How To Run the Examples
1. Start a Sling launchpad
2. Install the `org.apache.sling.validation.api` and `org.apache.sling.validation.core` bundles:
    
    ```bash
    cd ../api
    mvn clean package sling:install
    cd ../core
    mvn clean package sling:install
    cd ../examples
    mvn clean package sling:install
    ```

## Invalid POST request
```bash
curl -u admin:admin -Fsling:resourceType=/apps/validationdemo/components/user -Fusername=johnsmith -FfirstName=John204 -FlastName=Smith http://127.0.0.1:8080/content/validationdemo/users/johnsmith.modify.html
```

Check that the resource has not been modified at http://127.0.0.1:8080/content/validationdemo/users/johnsmith.html.

## Valid POST request
```bash
curl -u admin:admin -Fsling:resourceType=/apps/validationdemo/components/user -Fusername=johnsmith -FfirstName=Johnny -FlastName=Bravo http://127.0.0.1:8080/content/validationdemo/users/johnsmith.modify.html
```

Check that the resource has been modified at http://127.0.0.1:8080/content/validationdemo/users/johnsmith.html.
